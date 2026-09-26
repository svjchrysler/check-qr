package almacen

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/google/uuid"
)

// Estos tests necesitan un Postgres de verdad. Se saltan si no hay uno, para que
// `go test ./...` siga siendo util en una maquina sin base, pero la idempotencia
// de la subida de pagos no se puede comprobar con un doble: es una garantia del
// motor, no del codigo Go.
//
//	createdb checkqr_test
//	psql -d checkqr_test -f migraciones/001_inicial.sql
//	CHECKQR_TEST_DB="postgres:///checkqr_test" go test ./internal/almacen/
func abrirParaTest(t *testing.T) *Almacen {
	t.Helper()

	url := os.Getenv("CHECKQR_TEST_DB")
	if url == "" {
		t.Skip("sin CHECKQR_TEST_DB: se salta el test de integracion")
	}

	alm, err := Nuevo(context.Background(), url)
	if err != nil {
		t.Fatalf("no se pudo conectar: %v", err)
	}
	t.Cleanup(alm.Cerrar)
	return alm
}

// orgDePrueba crea un comercio con un usuario, y lo limpia al terminar.
func orgDePrueba(t *testing.T, alm *Almacen) (orgID, usuarioID string) {
	t.Helper()
	ctx := context.Background()

	orgID = uuid.NewString()
	usuarioID = uuid.NewString()

	_, err := alm.pool.Exec(ctx, `INSERT INTO orgs (id, nombre) VALUES ($1, $2)`, orgID, "Tienda de prueba")
	if err != nil {
		t.Fatalf("creando org: %v", err)
	}
	_, err = alm.pool.Exec(ctx,
		`INSERT INTO usuarios (id, google_sub, email, nombre) VALUES ($1,$2,$3,$4)`,
		usuarioID, "sub-"+usuarioID, "prueba@ejemplo.com", "Duena de prueba")
	if err != nil {
		t.Fatalf("creando usuario: %v", err)
	}
	_, err = alm.pool.Exec(ctx,
		`INSERT INTO membresias (org_id, usuario_id, rol) VALUES ($1,$2,'dueno')`, orgID, usuarioID)
	if err != nil {
		t.Fatalf("creando membresia: %v", err)
	}

	t.Cleanup(func() {
		_, _ = alm.pool.Exec(context.Background(), `DELETE FROM orgs WHERE id = $1`, orgID)
		_, _ = alm.pool.Exec(context.Background(), `DELETE FROM usuarios WHERE id = $1`, usuarioID)
	})
	return orgID, usuarioID
}

func pagoDePrueba(orgID string, dedupKey string) Pago {
	pagador := "Juan Perez"
	return Pago{
		ID:              uuid.NewString(),
		OrgID:           orgID,
		DedupKey:        dedupKey,
		Wallet:          "yape",
		MontoCentavos:   5000,
		Moneda:          "BOB",
		Pagador:         &pagador,
		AvisoEn:         time.Now().UTC().Truncate(time.Millisecond),
		Nivel:           "aviso_banco",
		Confianza:       "parcial",
		SubidoPorDevice: "device-1",
	}
}

// El test que justifica que el servidor exista tal como esta: subir el mismo
// pago dos veces tiene que dar un solo pago. Sin eso, el modo offline de la app
// duplicaria cobros cada vez que se corta la red a mitad de una subida.
func TestSubirElMismoPagoDosVecesGuardaUno(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgID, _ := orgDePrueba(t, alm)

	p := pagoDePrueba(orgID, "com.banco.yape|5000|REF1|28333333")

	nuevo, err := alm.GuardarPago(ctx, p)
	if err != nil {
		t.Fatalf("primera subida: %v", err)
	}
	if !nuevo {
		t.Fatal("la primera subida deberia ser nueva")
	}

	// Segunda subida: mismo dedup_key, otro id (el celular reintento).
	p2 := p
	p2.ID = uuid.NewString()
	nuevo, err = alm.GuardarPago(ctx, p2)
	if err != nil {
		t.Fatalf("segunda subida: %v", err)
	}
	if nuevo {
		t.Fatal("la segunda subida no deberia contarse como nueva; se reenviaria el push dos veces")
	}

	pagos, err := alm.PagosDelDia(ctx, orgID,
		time.Now().Add(-time.Hour), time.Now().Add(time.Hour))
	if err != nil {
		t.Fatalf("leyendo: %v", err)
	}
	if len(pagos) != 1 {
		t.Fatalf("se esperaba 1 pago, hay %d", len(pagos))
	}
}

func TestDosComerciosNoSePisanConLaMismaClave(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgA, _ := orgDePrueba(t, alm)
	orgB, _ := orgDePrueba(t, alm)

	// La clave de dedup la calcula la app y no es global: dos comercios distintos
	// pueden producir la misma. El UNIQUE incluye org_id justamente por esto.
	clave := "com.banco.yape|5000||28333333"

	if nuevo, err := alm.GuardarPago(ctx, pagoDePrueba(orgA, clave)); err != nil || !nuevo {
		t.Fatalf("comercio A: nuevo=%v err=%v", nuevo, err)
	}
	if nuevo, err := alm.GuardarPago(ctx, pagoDePrueba(orgB, clave)); err != nil || !nuevo {
		t.Fatalf("comercio B deberia poder guardar la misma clave: nuevo=%v err=%v", nuevo, err)
	}
}

func TestElRangoDelDiaEsSemiabierto(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgID, _ := orgDePrueba(t, alm)

	limite := time.Date(2026, 3, 10, 4, 0, 0, 0, time.UTC)

	antes := pagoDePrueba(orgID, "clave-antes")
	antes.AvisoEn = limite.Add(-time.Millisecond)
	justo := pagoDePrueba(orgID, "clave-justo")
	justo.AvisoEn = limite

	if _, err := alm.GuardarPago(ctx, antes); err != nil {
		t.Fatal(err)
	}
	if _, err := alm.GuardarPago(ctx, justo); err != nil {
		t.Fatal(err)
	}

	primerDia, err := alm.PagosDelDia(ctx, orgID, limite.Add(-time.Hour), limite)
	if err != nil {
		t.Fatal(err)
	}
	segundoDia, err := alm.PagosDelDia(ctx, orgID, limite, limite.Add(time.Hour))
	if err != nil {
		t.Fatal(err)
	}

	if len(primerDia) != 1 || primerDia[0].DedupKey != "clave-antes" {
		t.Fatalf("el primer dia deberia tener solo el de antes, tiene %d", len(primerDia))
	}
	if len(segundoDia) != 1 || segundoDia[0].DedupKey != "clave-justo" {
		t.Fatalf("el segundo dia deberia tener solo el del limite, tiene %d", len(segundoDia))
	}
}

func TestSoloSeReenviaALosQueTienenLaCajaAbierta(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgID, usuarioID := orgDePrueba(t, alm)

	tokenDueno := "token-dueno"
	tokenCajero := "token-cajero"
	tokenCerrado := "token-cerrado"

	for deviceID, token := range map[string]string{
		"dev-dueno":   tokenDueno,
		"dev-cajero":  tokenCajero,
		"dev-cerrado": tokenCerrado,
	} {
		tk := token
		err := alm.RegistrarDispositivo(ctx, Dispositivo{
			ID: uuid.NewString(), OrgID: orgID, UsuarioID: usuarioID,
			DeviceID: deviceID, FCMToken: &tk,
		})
		if err != nil {
			t.Fatalf("registrando %s: %v", deviceID, err)
		}
	}

	// Caja abierta en el dueno y en un cajero; el tercero la tiene cerrada.
	if err := alm.FijarCajaAbierta(ctx, orgID, "dev-dueno", true); err != nil {
		t.Fatal(err)
	}
	if err := alm.FijarCajaAbierta(ctx, orgID, "dev-cajero", true); err != nil {
		t.Fatal(err)
	}

	// El pago lo subio el dueno: no se le reenvia a si mismo.
	tokens, err := alm.TokensParaReenvio(ctx, orgID, "dev-dueno")
	if err != nil {
		t.Fatal(err)
	}
	if len(tokens) != 1 || tokens[0] != tokenCajero {
		t.Fatalf("se esperaba solo el token del cajero con caja abierta, salio %v", tokens)
	}
}

func TestRegistrarDispositivoActualizaElToken(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgID, usuarioID := orgDePrueba(t, alm)

	viejo, nuevo := "token-viejo", "token-nuevo"

	base := Dispositivo{
		ID: uuid.NewString(), OrgID: orgID, UsuarioID: usuarioID,
		DeviceID: "dev-1", FCMToken: &viejo,
	}
	if err := alm.RegistrarDispositivo(ctx, base); err != nil {
		t.Fatal(err)
	}

	// FCM rota los tokens: volver a registrar tiene que actualizar, no duplicar.
	base.ID = uuid.NewString()
	base.FCMToken = &nuevo
	if err := alm.RegistrarDispositivo(ctx, base); err != nil {
		t.Fatal(err)
	}

	if err := alm.FijarCajaAbierta(ctx, orgID, "dev-1", true); err != nil {
		t.Fatal(err)
	}
	tokens, err := alm.TokensParaReenvio(ctx, orgID, "otro-device")
	if err != nil {
		t.Fatal(err)
	}
	if len(tokens) != 1 || tokens[0] != nuevo {
		t.Fatalf("se esperaba el token nuevo y solo uno, salio %v", tokens)
	}
}

func TestLasPlantillasSoloSeEntreganSiHayUnaMasNueva(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()

	contenido := []byte(`{"version":42,"generado_en_millis":1,"templates":[]}`)
	if err := alm.PublicarPlantillas(ctx, 42, contenido, "canonico", "firma"); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		_, _ = alm.pool.Exec(context.Background(), `DELETE FROM plantillas WHERE version = 42`)
	})

	if p, err := alm.UltimasPlantillas(ctx, 0); err != nil || p.Version != 42 {
		t.Fatalf("se esperaba la version 42: %+v err=%v", p, err)
	}

	// El celular ya esta al dia: no hay nada que bajar.
	if _, err := alm.UltimasPlantillas(ctx, 42); err != ErrNoEncontrado {
		t.Fatalf("se esperaba ErrNoEncontrado, salio %v", err)
	}
}

func TestNoSeAceptaUnPagoConMontoCero(t *testing.T) {
	alm := abrirParaTest(t)
	ctx := context.Background()
	orgID, _ := orgDePrueba(t, alm)

	// La restriccion esta en la base y no solo en el handler: un pago de cero es
	// siempre un error de parseo, y conviene que no pueda entrar por ningun lado.
	p := pagoDePrueba(orgID, "clave-cero")
	p.MontoCentavos = 0

	if _, err := alm.GuardarPago(ctx, p); err == nil {
		t.Fatal("la base deberia rechazar un monto de cero")
	}
}
