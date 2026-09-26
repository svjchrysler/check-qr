// Servicio HTTP de CheckQr.
package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/seef/checkqr/server/internal/almacen"
	"github.com/seef/checkqr/server/internal/api"
	"github.com/seef/checkqr/server/internal/auth"
	"github.com/seef/checkqr/server/internal/push"
)

func main() {
	log := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))

	if err := correr(log); err != nil {
		log.Error("el servicio termino con error", "error", err)
		os.Exit(1)
	}
}

func correr(log *slog.Logger) error {
	ctx, cancelar := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancelar()

	urlBase := obligatoria("DATABASE_URL")
	secretoJWT := obligatoria("JWT_SECRET")
	clienteGoogle := obligatoria("GOOGLE_CLIENT_ID")

	alm, err := almacen.Nuevo(ctx, urlBase)
	if err != nil {
		return err
	}
	defer alm.Cerrar()

	enviador := construirEnviador(ctx, log)
	verificador := auth.NuevoVerificador(clienteGoogle, []byte(secretoJWT))
	servidor := api.Nuevo(alm, verificador, enviador, log)

	puerto := os.Getenv("PORT")
	if puerto == "" {
		puerto = "8080" // lo que espera Cloud Run
	}

	srv := &http.Server{
		Addr:              ":" + puerto,
		Handler:           servidor.Rutas(),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       2 * time.Minute,
	}

	go func() {
		<-ctx.Done()
		// Apagado ordenado: se le da tiempo a las peticiones en vuelo a
		// terminar, para no perder la subida de un pago por un despliegue.
		apagado, cancelarApagado := context.WithTimeout(context.Background(), 20*time.Second)
		defer cancelarApagado()
		_ = srv.Shutdown(apagado)
	}()

	log.Info("escuchando", "puerto", puerto)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		return err
	}
	log.Info("apagado limpio")
	return nil
}

// construirEnviador devuelve el enviador real si hay credenciales, y el que
// descarta si no. Sin credenciales el servicio arranca igual: es lo correcto
// para desarrollo, y en produccion la ausencia queda en el log.
func construirEnviador(ctx context.Context, log *slog.Logger) push.Enviador {
	proyecto := os.Getenv("FIREBASE_PROJECT_ID")
	credenciales := os.Getenv("FIREBASE_CREDENTIALS_JSON")
	if proyecto == "" || credenciales == "" {
		log.Warn("sin credenciales de FCM: los pagos no se reenviaran a los cajeros")
		return push.EnviadorNoOp{}
	}

	enviador, err := push.NuevoEnviadorFCM(ctx, proyecto, []byte(credenciales))
	if err != nil {
		log.Error("no se pudo crear el enviador de FCM, se sigue sin reenvio", "error", err)
		return push.EnviadorNoOp{}
	}
	return enviador
}

func obligatoria(nombre string) string {
	v := os.Getenv(nombre)
	if v == "" {
		slog.Error("falta la variable de entorno", "nombre", nombre)
		os.Exit(1)
	}
	return v
}
