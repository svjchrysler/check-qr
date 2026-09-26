package plantillas

import (
	"crypto/ed25519"
	"encoding/base64"
	"encoding/json"
	"testing"
)

// El mismo vector que usa VerificadorEd25519Test del lado de Kotlin.
//
// La semilla es fija (bytes 1..32) y NO es una clave real. Si este test y el de
// Kotlin dejan de coincidir, las plantillas dejarian de actualizarse en
// produccion sin que nada falle a la vista: de ahi que el vector este duplicado
// a proposito en los dos lados en vez de generarse.
const (
	clavePublicaEsperada = "ebVWLo/mVPlAeLES6KmLp5AfhTrmlb7X4OORC60ElmQ="
	firmaEsperada        = "iwqcuz2DDvp/CiEdJjNI2LWDDpyh3BUia6iuNRo0vK2OXgXhNVvAeOl4AhqEVvb5MQwx/998yDr0Sf/b9nohDw=="
	canonicoEsperado     = `1:7;13:1700000000000;1:1;4:yape;14:com.banco.yape;0:;30:recibiste Bs (?<monto>[\d.,]+);4:AUTO;0:;2:10;`
)

func claveDePrueba() ed25519.PrivateKey {
	semilla := make([]byte, ed25519.SeedSize)
	for i := range semilla {
		semilla[i] = byte(i + 1)
	}
	return ed25519.NewKeyFromSeed(semilla)
}

func paqueteDePrueba() Paquete {
	return Paquete{
		Version:          7,
		GeneradoEnMillis: 1700000000000,
		Templates: []Plantilla{
			{
				Wallet:          "YAPE",
				Packages:        []string{"com.banco.yape"},
				PatronesTitulo:  nil,
				PatronCuerpo:    `recibiste Bs (?<monto>[\d.,]+)`,
				FormatoMonto:    "auto",
				PatronesExcluir: nil,
				Prioridad:       10,
			},
		},
	}
}

func TestCanonicoCoincideConKotlin(t *testing.T) {
	obtenido := paqueteDePrueba().Canonico()
	if obtenido != canonicoEsperado {
		t.Fatalf("la forma canonica se separo de la de Kotlin.\n esperado: %q\n obtenido: %q",
			canonicoEsperado, obtenido)
	}
}

func TestFirmaCoincideConLaQueAceptaLaApp(t *testing.T) {
	firmado, err := Firmar(paqueteDePrueba(), claveDePrueba())
	if err != nil {
		t.Fatalf("no se pudo firmar: %v", err)
	}
	if firmado.FirmaBase64 != firmaEsperada {
		t.Fatalf("la firma cambio; la app la rechazaria.\n esperada: %s\n obtenida: %s",
			firmaEsperada, firmado.FirmaBase64)
	}
}

func TestLaClavePublicaEsLaQueLlevaLaApp(t *testing.T) {
	pub := claveDePrueba().Public().(ed25519.PublicKey)
	obtenida := base64.StdEncoding.EncodeToString(pub)
	if obtenida != clavePublicaEsperada {
		t.Fatalf("clave publica distinta: %s", obtenida)
	}
}

func TestVerificarAceptaLoPropio(t *testing.T) {
	clave := claveDePrueba()
	firmado, _ := Firmar(paqueteDePrueba(), clave)
	if !Verificar(firmado, clave.Public().(ed25519.PublicKey)) {
		t.Fatal("no se verifica la propia firma")
	}
}

func TestVerificarRechazaContenidoAlterado(t *testing.T) {
	clave := claveDePrueba()
	firmado, _ := Firmar(paqueteDePrueba(), clave)

	// Este es el ataque que la firma existe para frenar.
	firmado.Bundle.Templates[0].PatronCuerpo = `(?<monto>[\d.,]+)`

	if Verificar(firmado, clave.Public().(ed25519.PublicKey)) {
		t.Fatal("se acepto un paquete con el patron cambiado")
	}
}

// --- Detalles que rompen la firma sin avisar ------------------------------

func TestElOrdenDeLasPlantillasNoCambiaLaFirma(t *testing.T) {
	clave := claveDePrueba()

	uno := Paquete{Version: 1, GeneradoEnMillis: 1, Templates: []Plantilla{
		{Wallet: "YAPE", Packages: []string{"a"}, PatronCuerpo: "x", FormatoMonto: "auto"},
		{Wallet: "BCP", Packages: []string{"b"}, PatronCuerpo: "y", FormatoMonto: "auto"},
	}}
	otro := Paquete{Version: 1, GeneradoEnMillis: 1, Templates: []Plantilla{
		{Wallet: "BCP", Packages: []string{"b"}, PatronCuerpo: "y", FormatoMonto: "auto"},
		{Wallet: "YAPE", Packages: []string{"a"}, PatronCuerpo: "x", FormatoMonto: "auto"},
	}}

	fUno, _ := Firmar(uno, clave)
	fOtro, _ := Firmar(otro, clave)
	if fUno.FirmaBase64 != fOtro.FirmaBase64 {
		t.Fatal("el orden de las plantillas cambio la firma; deberia normalizarse")
	}
}

func TestElOrdenDeLosPaquetesNoCambiaLaFirma(t *testing.T) {
	clave := claveDePrueba()

	uno := Paquete{Version: 1, GeneradoEnMillis: 1, Templates: []Plantilla{
		{Wallet: "YAPE", Packages: []string{"com.a", "com.b"}, PatronCuerpo: "x", FormatoMonto: "auto"},
	}}
	otro := Paquete{Version: 1, GeneradoEnMillis: 1, Templates: []Plantilla{
		{Wallet: "YAPE", Packages: []string{"com.b", "com.a"}, PatronCuerpo: "x", FormatoMonto: "auto"},
	}}

	fUno, _ := Firmar(uno, clave)
	fOtro, _ := Firmar(otro, clave)
	if fUno.FirmaBase64 != fOtro.FirmaBase64 {
		t.Fatal("el orden de los paquetes cambio la firma")
	}
}

func TestFormatoMontoUsaElNombreDelEnumDeKotlin(t *testing.T) {
	// Kotlin firma sobre AmountFormat.name, no sobre el @SerialName en
	// minusculas que viaja en el JSON.
	casos := map[string]string{
		"auto":          "AUTO",
		"coma_decimal":  "COMA_DECIMAL",
		"punto_decimal": "PUNTO_DECIMAL",
		"":              "AUTO",
	}
	for entrada, esperado := range casos {
		if got := formatoMontoCanonico(entrada); got != esperado {
			t.Errorf("formatoMontoCanonico(%q) = %q, se esperaba %q", entrada, got, esperado)
		}
	}
}

// --- El JSON tiene que ser el que la app sabe leer -------------------------

func TestLasEtiquetasJSONCoincidenConKotlin(t *testing.T) {
	crudo, err := json.Marshal(paqueteDePrueba())
	if err != nil {
		t.Fatalf("no se pudo serializar: %v", err)
	}

	var comoMapa map[string]any
	if err := json.Unmarshal(crudo, &comoMapa); err != nil {
		t.Fatalf("json invalido: %v", err)
	}

	for _, clave := range []string{"version", "generado_en_millis", "templates"} {
		if _, ok := comoMapa[clave]; !ok {
			t.Errorf("falta la clave %q que espera kotlinx.serialization", clave)
		}
	}

	plantilla := comoMapa["templates"].([]any)[0].(map[string]any)
	for _, clave := range []string{
		"wallet", "packages", "patrones_titulo", "patron_cuerpo",
		"formato_monto", "patrones_excluir", "prioridad",
	} {
		if _, ok := plantilla[clave]; !ok {
			t.Errorf("falta la clave %q en la plantilla", clave)
		}
	}
}
