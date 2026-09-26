// Package auth resuelve quien es quien.
//
// El celular inicia sesion con Google (Credential Manager) y manda su ID token.
// El servidor lo verifica contra Google una sola vez y emite su propio token
// corto, que es el que viaja en las peticiones siguientes. Asi no se depende de
// una llamada a Google en cada request.
package auth

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"google.golang.org/api/idtoken"
)

var (
	ErrSinCredencial = errors.New("falta la credencial")
	ErrCredencialMal = errors.New("credencial invalida")
)

// Identidad es quien hace la peticion, ya resuelto.
type Identidad struct {
	UsuarioID string
	OrgID     string
	Rol       string
}

// Puede dice si el rol alcanza para gestionar el equipo.
//
// Se modela como metodo y no como comparacion suelta para que la regla viva en
// un solo sitio: si manana el encargado puede invitar cajeros, se cambia aqui.
func (i Identidad) PuedeGestionarEquipo() bool { return i.Rol == "dueno" }

func (i Identidad) PuedeVerCuadreCompleto() bool { return i.Rol != "cajero" }

// Verificador comprueba los ID tokens de Google y emite los propios.
type Verificador struct {
	clienteGoogleID string
	secretoJWT      []byte
	duracion        time.Duration
}

func NuevoVerificador(clienteGoogleID string, secretoJWT []byte) *Verificador {
	return &Verificador{
		clienteGoogleID: clienteGoogleID,
		secretoJWT:      secretoJWT,
		// Corto a proposito: si un token se filtra, la ventana es chica. El
		// celular lo renueva con su ID token de Google, que ya tiene.
		duracion: time.Hour,
	}
}

// DatosDeGoogle es lo que se saca del ID token.
type DatosDeGoogle struct {
	Sub    string
	Email  string
	Nombre string
}

// VerificarGoogle valida el ID token contra las claves publicas de Google.
func (v *Verificador) VerificarGoogle(ctx context.Context, idToken string) (DatosDeGoogle, error) {
	payload, err := idtoken.Validate(ctx, idToken, v.clienteGoogleID)
	if err != nil {
		return DatosDeGoogle{}, ErrCredencialMal
	}

	sub, _ := payload.Claims["sub"].(string)
	if sub == "" {
		return DatosDeGoogle{}, ErrCredencialMal
	}
	email, _ := payload.Claims["email"].(string)
	nombre, _ := payload.Claims["name"].(string)

	return DatosDeGoogle{Sub: sub, Email: email, Nombre: nombre}, nil
}

type reclamos struct {
	OrgID string `json:"org"`
	Rol   string `json:"rol"`
	jwt.RegisteredClaims
}

// EmitirToken crea el token propio del servicio.
func (v *Verificador) EmitirToken(usuarioID, orgID, rol string) (string, time.Time, error) {
	expira := time.Now().Add(v.duracion)
	t := jwt.NewWithClaims(jwt.SigningMethodHS256, reclamos{
		OrgID: orgID,
		Rol:   rol,
		RegisteredClaims: jwt.RegisteredClaims{
			Subject:   usuarioID,
			ExpiresAt: jwt.NewNumericDate(expira),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "checkqr",
		},
	})
	firmado, err := t.SignedString(v.secretoJWT)
	return firmado, expira, err
}

func (v *Verificador) leerToken(token string) (Identidad, error) {
	var c reclamos
	_, err := jwt.ParseWithClaims(token, &c, func(t *jwt.Token) (any, error) {
		// Se fija el algoritmo: aceptar el que diga la cabecera permitiria un
		// token con alg=none.
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, ErrCredencialMal
		}
		return v.secretoJWT, nil
	}, jwt.WithIssuer("checkqr"), jwt.WithValidMethods([]string{"HS256"}))

	if err != nil {
		return Identidad{}, ErrCredencialMal
	}
	if c.Subject == "" || c.OrgID == "" {
		return Identidad{}, ErrCredencialMal
	}
	return Identidad{UsuarioID: c.Subject, OrgID: c.OrgID, Rol: c.Rol}, nil
}

type claveDeContexto struct{}

// Middleware exige un token valido y deja la identidad en el contexto.
func (v *Verificador) Middleware(siguiente http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		cabecera := r.Header.Get("Authorization")
		if !strings.HasPrefix(cabecera, "Bearer ") {
			http.Error(w, "falta la credencial", http.StatusUnauthorized)
			return
		}

		id, err := v.leerToken(strings.TrimPrefix(cabecera, "Bearer "))
		if err != nil {
			http.Error(w, "credencial invalida", http.StatusUnauthorized)
			return
		}

		ctx := context.WithValue(r.Context(), claveDeContexto{}, id)
		siguiente.ServeHTTP(w, r.WithContext(ctx))
	})
}

// De saca la identidad del contexto.
func De(ctx context.Context) (Identidad, bool) {
	id, ok := ctx.Value(claveDeContexto{}).(Identidad)
	return id, ok
}
