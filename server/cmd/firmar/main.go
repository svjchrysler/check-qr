// Herramienta para firmar y publicar un paquete de plantillas.
//
// Es la que se usa cuando un banco cambia el formato de sus avisos: se corrige
// el JSON, se firma, se publica, y los celulares lo bajan solos. Eso es lo que
// evita publicar una version nueva de la app por un cambio de texto.
//
//	# generar el par de claves (una sola vez; la privada va a Secret Manager)
//	go run ./cmd/firmar -generar-claves
//
//	# firmar y publicar
//	CHECKQR_CLAVE_PRIVADA=<base64> DATABASE_URL=... \
//	  go run ./cmd/firmar -archivo plantillas.json
package main

import (
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"os"

	"github.com/seef/checkqr/server/internal/almacen"
	"github.com/seef/checkqr/server/internal/plantillas"
)

func main() {
	generar := flag.Bool("generar-claves", false, "genera un par Ed25519 y sale")
	archivo := flag.String("archivo", "", "JSON del paquete de plantillas")
	soloFirmar := flag.Bool("solo-firmar", false, "imprime el paquete firmado sin publicarlo")
	flag.Parse()

	if *generar {
		if err := generarClaves(); err != nil {
			salir(err)
		}
		return
	}
	if *archivo == "" {
		salir(fmt.Errorf("falta -archivo"))
	}
	if err := firmarYPublicar(*archivo, *soloFirmar); err != nil {
		salir(err)
	}
}

func generarClaves() error {
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return err
	}
	fmt.Println("# La privada va a Secret Manager. NUNCA al repositorio.")
	fmt.Printf("CHECKQR_CLAVE_PRIVADA=%s\n", base64.StdEncoding.EncodeToString(priv))
	fmt.Println()
	fmt.Println("# La publica va fijada en el binario de la app, en")
	fmt.Println("# core/network/build.gradle.kts -> CLAVE_PUBLICA_PLANTILLAS")
	fmt.Printf("CLAVE_PUBLICA=%s\n", base64.StdEncoding.EncodeToString(pub))
	return nil
}

func firmarYPublicar(ruta string, soloFirmar bool) error {
	crudo, err := os.ReadFile(ruta)
	if err != nil {
		return err
	}

	var paquete plantillas.Paquete
	if err := json.Unmarshal(crudo, &paquete); err != nil {
		return fmt.Errorf("el JSON del paquete no es valido: %w", err)
	}
	if paquete.Version <= 0 {
		return fmt.Errorf("la version tiene que ser mayor que 0; la 0 significa 'plantillas sin verificar'")
	}

	privB64 := os.Getenv("CHECKQR_CLAVE_PRIVADA")
	if privB64 == "" {
		return fmt.Errorf("falta CHECKQR_CLAVE_PRIVADA")
	}
	priv, err := base64.StdEncoding.DecodeString(privB64)
	if err != nil {
		return fmt.Errorf("CHECKQR_CLAVE_PRIVADA no es base64 valido: %w", err)
	}

	firmado, err := plantillas.Firmar(paquete, ed25519.PrivateKey(priv))
	if err != nil {
		return err
	}

	// Se comprueba la firma antes de publicar: publicar un paquete que la app va
	// a rechazar la dejaria con las plantillas viejas sin ninguna senal de error.
	pub := ed25519.PrivateKey(priv).Public().(ed25519.PublicKey)
	if !plantillas.Verificar(firmado, pub) {
		return fmt.Errorf("la firma recien hecha no verifica; no se publica")
	}

	if soloFirmar {
		return json.NewEncoder(os.Stdout).Encode(firmado)
	}

	urlBase := os.Getenv("DATABASE_URL")
	if urlBase == "" {
		return fmt.Errorf("falta DATABASE_URL")
	}
	ctx := context.Background()
	alm, err := almacen.Nuevo(ctx, urlBase)
	if err != nil {
		return err
	}
	defer alm.Cerrar()

	// Se guarda el JSON tal cual entro, no una reserializacion: la firma se
	// calculo sobre este contenido exacto.
	if err := alm.PublicarPlantillas(ctx, paquete.Version, crudo, paquete.Canonico(), firmado.FirmaBase64); err != nil {
		return err
	}

	fmt.Printf("publicada la version %d con %d plantillas\n", paquete.Version, len(paquete.Templates))
	return nil
}

func salir(err error) {
	fmt.Fprintln(os.Stderr, "error:", err)
	os.Exit(1)
}
