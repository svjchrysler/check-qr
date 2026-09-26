// Package almacen habla con Postgres.
package almacen

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// ErrNoEncontrado lo devuelven las consultas que esperaban una fila.
var ErrNoEncontrado = errors.New("no encontrado")

type Almacen struct {
	pool *pgxpool.Pool
}

func Nuevo(ctx context.Context, urlBase string) (*Almacen, error) {
	cfg, err := pgxpool.ParseConfig(urlBase)
	if err != nil {
		return nil, err
	}
	// El servicio hace consultas cortas y frecuentes: mas vale un pool modesto
	// con conexiones que se reciclan que uno grande que envejece.
	cfg.MaxConns = 10
	cfg.MaxConnLifetime = 30 * time.Minute
	cfg.HealthCheckPeriod = time.Minute

	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		return nil, err
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, err
	}
	return &Almacen{pool: pool}, nil
}

func (a *Almacen) Cerrar() { a.pool.Close() }

func (a *Almacen) Ping(ctx context.Context) error { return a.pool.Ping(ctx) }

// --- Pagos -----------------------------------------------------------------

type Pago struct {
	ID              string
	OrgID           string
	DedupKey        string
	Wallet          string
	MontoCentavos   int64
	Moneda          string
	Pagador         *string
	Referencia      *string
	AvisoEn         time.Time
	Nivel           string
	Confianza       string
	TurnoID         *string
	CajeroID        *string
	SubidoPorDevice string
}

// GuardarPago inserta el pago si es nuevo.
//
// El ON CONFLICT DO NOTHING sobre (org_id, dedup_key) es la segunda linea de
// defensa contra los duplicados, despues de la de la app. Importa porque hace
// que subir un pago sea idempotente: el celular puede reintentar una subida que
// en realidad si llego, que es exactamente lo que pasa cuando se corta la red a
// mitad de la peticion.
//
// Devuelve nuevo=false si ya existia, para no reenviar el push dos veces.
func (a *Almacen) GuardarPago(ctx context.Context, p Pago) (nuevo bool, err error) {
	const q = `
		INSERT INTO pagos (
			id, org_id, dedup_key, wallet, monto_centavos, moneda,
			pagador, referencia, aviso_en, nivel, confianza,
			turno_id, cajero_id, subido_por_device
		) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)
		ON CONFLICT (org_id, dedup_key) DO NOTHING
		RETURNING id`

	var id string
	err = a.pool.QueryRow(ctx, q,
		p.ID, p.OrgID, p.DedupKey, p.Wallet, p.MontoCentavos, p.Moneda,
		p.Pagador, p.Referencia, p.AvisoEn, p.Nivel, p.Confianza,
		p.TurnoID, p.CajeroID, p.SubidoPorDevice,
	).Scan(&id)

	if errors.Is(err, pgx.ErrNoRows) {
		return false, nil // ya existia
	}
	if err != nil {
		return false, err
	}
	return true, nil
}

// PagosDelDia devuelve los pagos de un rango, para el cuadre.
//
// El rango es semiabierto [desde, hasta) para que dos dias consecutivos no se
// solapen ni dejen un instante afuera.
func (a *Almacen) PagosDelDia(ctx context.Context, orgID string, desde, hasta time.Time) ([]Pago, error) {
	const q = `
		SELECT id, org_id, dedup_key, wallet, monto_centavos, moneda,
		       pagador, referencia, aviso_en, nivel, confianza,
		       turno_id, cajero_id, subido_por_device
		FROM pagos
		WHERE org_id = $1 AND aviso_en >= $2 AND aviso_en < $3
		ORDER BY aviso_en DESC`

	filas, err := a.pool.Query(ctx, q, orgID, desde, hasta)
	if err != nil {
		return nil, err
	}
	defer filas.Close()

	var pagos []Pago
	for filas.Next() {
		var p Pago
		if err := filas.Scan(
			&p.ID, &p.OrgID, &p.DedupKey, &p.Wallet, &p.MontoCentavos, &p.Moneda,
			&p.Pagador, &p.Referencia, &p.AvisoEn, &p.Nivel, &p.Confianza,
			&p.TurnoID, &p.CajeroID, &p.SubidoPorDevice,
		); err != nil {
			return nil, err
		}
		pagos = append(pagos, p)
	}
	return pagos, filas.Err()
}

// --- Dispositivos -----------------------------------------------------------

type Dispositivo struct {
	ID          string
	OrgID       string
	UsuarioID   string
	DeviceID    string
	FCMToken    *string
	CajaAbierta bool
}

// RegistrarDispositivo crea o actualiza el celular y su token de FCM.
func (a *Almacen) RegistrarDispositivo(ctx context.Context, d Dispositivo) error {
	const q = `
		INSERT INTO dispositivos (id, org_id, usuario_id, device_id, fcm_token, visto_en)
		VALUES ($1,$2,$3,$4,$5, now())
		ON CONFLICT (org_id, device_id) DO UPDATE
		SET fcm_token = EXCLUDED.fcm_token,
		    usuario_id = EXCLUDED.usuario_id,
		    visto_en = now()`
	_, err := a.pool.Exec(ctx, q, d.ID, d.OrgID, d.UsuarioID, d.DeviceID, d.FCMToken)
	return err
}

func (a *Almacen) FijarCajaAbierta(ctx context.Context, orgID, deviceID string, abierta bool) error {
	const q = `UPDATE dispositivos SET caja_abierta = $3, visto_en = now()
	           WHERE org_id = $1 AND device_id = $2`
	_, err := a.pool.Exec(ctx, q, orgID, deviceID, abierta)
	return err
}

// TokensParaReenvio devuelve los tokens de FCM de los celulares con la caja
// abierta, salvo el que subio el pago: ese ya lo tiene.
//
// Se filtra por caja abierta porque mandarle un push a un celular guardado en un
// cajon gasta bateria y no le sirve a nadie.
func (a *Almacen) TokensParaReenvio(ctx context.Context, orgID, exceptoDeviceID string) ([]string, error) {
	const q = `
		SELECT fcm_token FROM dispositivos
		WHERE org_id = $1 AND caja_abierta AND fcm_token IS NOT NULL
		  AND device_id <> $2`

	filas, err := a.pool.Query(ctx, q, orgID, exceptoDeviceID)
	if err != nil {
		return nil, err
	}
	defer filas.Close()

	var tokens []string
	for filas.Next() {
		var t string
		if err := filas.Scan(&t); err != nil {
			return nil, err
		}
		tokens = append(tokens, t)
	}
	return tokens, filas.Err()
}

// --- Plantillas --------------------------------------------------------------

type PaquetePublicado struct {
	Version      int
	ContenidoRaw []byte
	FirmaBase64  string
}

func (a *Almacen) PublicarPlantillas(ctx context.Context, version int, contenido []byte, canonico, firma string) error {
	const q = `
		INSERT INTO plantillas (version, contenido, canonico, firma_base64)
		VALUES ($1,$2,$3,$4)
		ON CONFLICT (version) DO NOTHING`
	_, err := a.pool.Exec(ctx, q, version, contenido, canonico, firma)
	return err
}

// UltimasPlantillas devuelve el paquete mas nuevo, si es posterior a
// desdeVersion. Devuelve ErrNoEncontrado si el celular ya esta al dia.
func (a *Almacen) UltimasPlantillas(ctx context.Context, desdeVersion int) (PaquetePublicado, error) {
	const q = `
		SELECT version, contenido, firma_base64 FROM plantillas
		WHERE version > $1
		ORDER BY version DESC LIMIT 1`

	var p PaquetePublicado
	err := a.pool.QueryRow(ctx, q, desdeVersion).Scan(&p.Version, &p.ContenidoRaw, &p.FirmaBase64)
	if errors.Is(err, pgx.ErrNoRows) {
		return PaquetePublicado{}, ErrNoEncontrado
	}
	return p, err
}

// --- Avisos no reconocidos ---------------------------------------------------

// GuardarAvisoNoReconocido guarda un aviso que ninguna plantilla caso.
//
// Solo llega aqui con consentimiento explicito del comerciante: el texto puede
// traer el nombre de una persona.
func (a *Almacen) GuardarAvisoNoReconocido(
	ctx context.Context, id, orgID, sourcePackage string, titulo, texto *string,
) error {
	const q = `
		INSERT INTO avisos_no_reconocidos (id, org_id, source_package, titulo, texto)
		VALUES ($1,$2,$3,$4,$5)`
	_, err := a.pool.Exec(ctx, q, id, orgID, sourcePackage, titulo, texto)
	return err
}
