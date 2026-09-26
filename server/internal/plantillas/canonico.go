// Package plantillas define el paquete de plantillas de banco y su firma.
//
// Las plantillas son expresiones regulares que la app ejecuta para decidir que
// aviso es un cobro y por cuanto. Quien pueda cambiarlas puede hacer que la app
// anuncie pagos que no existen, asi que van firmadas con Ed25519 y la app
// verifica la firma antes de escribir nada en disco.
package plantillas

import (
	"crypto/ed25519"
	"encoding/base64"
	"fmt"
	"sort"
	"strconv"
	"strings"
)

// Plantilla describe como reconocer el aviso de un banco.
//
// Las etiquetas JSON tienen que coincidir exactamente con los @SerialName del
// lado de Kotlin, en core/model/BankTemplate.kt.
type Plantilla struct {
	Wallet          string   `json:"wallet"`
	Packages        []string `json:"packages"`
	PatronesTitulo  []string `json:"patrones_titulo"`
	PatronCuerpo    string   `json:"patron_cuerpo"`
	FormatoMonto    string   `json:"formato_monto"`
	PatronesExcluir []string `json:"patrones_excluir"`
	Prioridad       int      `json:"prioridad"`
}

// Paquete es el conjunto completo de plantillas de una version.
type Paquete struct {
	Version          int         `json:"version"`
	GeneradoEnMillis int64       `json:"generado_en_millis"`
	Templates        []Plantilla `json:"templates"`
}

// PaqueteFirmado es lo que viaja al celular.
type PaqueteFirmado struct {
	Bundle      Paquete `json:"bundle"`
	FirmaBase64 string  `json:"firma_base64"`
}

// formatoMontoCanonico traduce el valor JSON al nombre del enum de Kotlin.
//
// Hace falta porque Kotlin firma sobre `AmountFormat.name` (AUTO, COMA_DECIMAL,
// PUNTO_DECIMAL) y no sobre el @SerialName en minusculas que usa el JSON. Es
// justo el tipo de diferencia que rompe la firma sin que nadie lo note.
func formatoMontoCanonico(valorJSON string) string {
	switch valorJSON {
	case "coma_decimal":
		return "COMA_DECIMAL"
	case "punto_decimal":
		return "PUNTO_DECIMAL"
	case "auto", "":
		return "AUTO"
	default:
		return strings.ToUpper(valorJSON)
	}
}

// walletCanonico traduce el nombre del enum de Kotlin a su id estable.
//
// Kotlin firma sobre `Wallet.id` (yape, bcp, ...), mientras que el JSON lleva el
// nombre del enum (YAPE, BCP, ...) porque asi lo serializa kotlinx por omision.
func walletCanonico(nombreEnum string) string {
	return strings.ToLower(nombreEnum)
}

// Canonico produce los bytes que se firman.
//
// Tiene que dar exactamente lo mismo que TemplateBundle.canonico en Kotlin. Cada
// campo va con su longitud por delante porque los campos son expresiones
// regulares y pueden contener cualquier caracter: un separador cualquiera seria
// ambiguo.
//
// Si esta funcion y la de Kotlin se separan, el sintoma no es un error: es que
// las plantillas dejan de actualizarse en produccion, en silencio. Por eso hay
// un test de interoperabilidad a cada lado con el mismo vector.
func (p Paquete) Canonico() string {
	var b strings.Builder

	campo := func(v string) {
		b.WriteString(strconv.Itoa(len(v)))
		b.WriteByte(':')
		b.WriteString(v)
		b.WriteByte(';')
	}

	campo(strconv.Itoa(p.Version))
	campo(strconv.FormatInt(p.GeneradoEnMillis, 10))
	campo(strconv.Itoa(len(p.Templates)))

	// Mismo orden que Kotlin: por id de billetera y despues por patron.
	ordenadas := make([]Plantilla, len(p.Templates))
	copy(ordenadas, p.Templates)
	sort.SliceStable(ordenadas, func(i, j int) bool {
		wi, wj := walletCanonico(ordenadas[i].Wallet), walletCanonico(ordenadas[j].Wallet)
		if wi != wj {
			return wi < wj
		}
		return ordenadas[i].PatronCuerpo < ordenadas[j].PatronCuerpo
	})

	for _, t := range ordenadas {
		paquetesOrdenados := make([]string, len(t.Packages))
		copy(paquetesOrdenados, t.Packages)
		sort.Strings(paquetesOrdenados)

		campo(walletCanonico(t.Wallet))
		campo(strings.Join(paquetesOrdenados, ","))
		campo(strings.Join(t.PatronesTitulo, ","))
		campo(t.PatronCuerpo)
		campo(formatoMontoCanonico(t.FormatoMonto))
		campo(strings.Join(t.PatronesExcluir, ","))
		campo(strconv.Itoa(t.Prioridad))
	}

	return b.String()
}

// Firmar produce el paquete listo para enviar al celular.
//
// La firma es Ed25519 crudo de 64 bytes, sin ninguna cabecera: es lo que espera
// la variante NO_PREFIX de Tink del lado de la app.
func Firmar(p Paquete, clave ed25519.PrivateKey) (PaqueteFirmado, error) {
	if len(clave) != ed25519.PrivateKeySize {
		return PaqueteFirmado{}, fmt.Errorf(
			"la clave privada mide %d bytes, se esperaban %d",
			len(clave), ed25519.PrivateKeySize,
		)
	}
	firma := ed25519.Sign(clave, []byte(p.Canonico()))
	return PaqueteFirmado{
		Bundle:      p,
		FirmaBase64: base64.StdEncoding.EncodeToString(firma),
	}, nil
}

// Verificar comprueba la firma. Existe para que los tests del servidor puedan
// ejercitar el mismo camino que recorre la app.
func Verificar(pf PaqueteFirmado, clavePublica ed25519.PublicKey) bool {
	firma, err := base64.StdEncoding.DecodeString(pf.FirmaBase64)
	if err != nil || len(firma) != ed25519.SignatureSize {
		return false
	}
	return ed25519.Verify(clavePublica, []byte(pf.Bundle.Canonico()), firma)
}
