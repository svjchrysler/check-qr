// Package push reenvia los pagos a los celulares de los cajeros.
package push

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"golang.org/x/oauth2/google"
)

// Enviador manda mensajes de datos a una lista de tokens de FCM.
type Enviador interface {
	Enviar(ctx context.Context, tokens []string, datos map[string]string) error
}

// EnviadorNoOp descarta todo. Es lo que se usa en desarrollo y en los tests:
// permite ejercitar el servicio entero sin credenciales de Firebase.
type EnviadorNoOp struct{}

func (EnviadorNoOp) Enviar(context.Context, []string, map[string]string) error { return nil }

// EnviadorFCM habla con la API HTTP v1 de Firebase Cloud Messaging.
//
// Se usa la API REST directamente y no el SDK de Firebase: el SDK arrastra media
// plataforma para lo unico que hace falta aqui, que es un POST autenticado.
type EnviadorFCM struct {
	proyectoID string
	cliente    *http.Client
}

func NuevoEnviadorFCM(ctx context.Context, proyectoID string, credencialesJSON []byte) (*EnviadorFCM, error) {
	cfg, err := google.JWTConfigFromJSON(credencialesJSON, "https://www.googleapis.com/auth/firebase.messaging")
	if err != nil {
		return nil, fmt.Errorf("credenciales de FCM invalidas: %w", err)
	}
	return &EnviadorFCM{
		proyectoID: proyectoID,
		cliente:    cfg.Client(ctx),
	}, nil
}

type mensaje struct {
	Message struct {
		Token   string            `json:"token"`
		Data    map[string]string `json:"data"`
		Android struct {
			// HIGH porque el cajero tiene que oir el pago en el momento; con
			// prioridad normal el sistema puede retrasarlo varios minutos.
			Priority string `json:"priority"`
		} `json:"android"`
	} `json:"message"`
}

func (e *EnviadorFCM) Enviar(ctx context.Context, tokens []string, datos map[string]string) error {
	url := fmt.Sprintf("https://fcm.googleapis.com/v1/projects/%s/messages:send", e.proyectoID)

	var primerError error
	for _, token := range tokens {
		var m mensaje
		m.Message.Token = token
		m.Message.Data = datos
		m.Message.Android.Priority = "high"

		cuerpo, err := json.Marshal(m)
		if err != nil {
			return err
		}

		ctxEnvio, cancelar := context.WithTimeout(ctx, 10*time.Second)
		req, err := http.NewRequestWithContext(ctxEnvio, http.MethodPost, url, bytes.NewReader(cuerpo))
		if err != nil {
			cancelar()
			return err
		}
		req.Header.Set("Content-Type", "application/json")

		resp, err := e.cliente.Do(req)
		cancelar()

		// Un token muerto no debe impedir que los demas cajeros reciban el pago:
		// se sigue con la lista y se reporta el primer fallo.
		if err != nil {
			if primerError == nil {
				primerError = err
			}
			continue
		}
		resp.Body.Close()
		if resp.StatusCode >= 300 && primerError == nil {
			primerError = fmt.Errorf("fcm respondio %d", resp.StatusCode)
		}
	}
	return primerError
}
