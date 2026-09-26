// Package api expone los endpoints HTTP.
package api

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"strconv"
	"time"

	"github.com/google/uuid"
	"github.com/seef/checkqr/server/internal/almacen"
	"github.com/seef/checkqr/server/internal/auth"
	"github.com/seef/checkqr/server/internal/push"
)

type Servidor struct {
	alm      *almacen.Almacen
	verif    *auth.Verificador
	enviador push.Enviador
	log      *slog.Logger
}

func Nuevo(alm *almacen.Almacen, verif *auth.Verificador, enviador push.Enviador, log *slog.Logger) *Servidor {
	return &Servidor{alm: alm, verif: verif, enviador: enviador, log: log}
}

// Rutas arma el enrutador.
//
// Se usa el ServeMux de la libreria estandar, que desde Go 1.22 entiende metodo
// y variables de ruta: una dependencia menos que mantener en un servicio que
// tiene ocho endpoints.
func (s *Servidor) Rutas() http.Handler {
	publico := http.NewServeMux()
	publico.HandleFunc("GET /salud", s.salud)
	publico.HandleFunc("POST /v1/sesion", s.iniciarSesion)

	privado := http.NewServeMux()
	privado.HandleFunc("POST /v1/dispositivos", s.registrarDispositivo)
	privado.HandleFunc("POST /v1/dispositivos/caja", s.fijarCajaAbierta)
	privado.HandleFunc("POST /v1/pagos", s.subirPago)
	privado.HandleFunc("GET /v1/plantillas", s.plantillas)
	privado.HandleFunc("GET /v1/reportes/dia", s.reporteDelDia)
	privado.HandleFunc("POST /v1/avisos-no-reconocidos", s.avisoNoReconocido)

	raiz := http.NewServeMux()
	raiz.Handle("/", publico)
	raiz.Handle("/v1/dispositivos", s.verif.Middleware(privado))
	raiz.Handle("/v1/dispositivos/caja", s.verif.Middleware(privado))
	raiz.Handle("/v1/pagos", s.verif.Middleware(privado))
	raiz.Handle("/v1/plantillas", s.verif.Middleware(privado))
	raiz.Handle("/v1/reportes/dia", s.verif.Middleware(privado))
	raiz.Handle("/v1/avisos-no-reconocidos", s.verif.Middleware(privado))

	return raiz
}

func (s *Servidor) salud(w http.ResponseWriter, r *http.Request) {
	if err := s.alm.Ping(r.Context()); err != nil {
		http.Error(w, "base no disponible", http.StatusServiceUnavailable)
		return
	}
	responder(w, http.StatusOK, map[string]string{"estado": "ok"})
}

// --- Sesion -------------------------------------------------------------------

type peticionSesion struct {
	IDTokenGoogle string `json:"id_token_google"`
}

type respuestaSesion struct {
	Token   string    `json:"token"`
	Expira  time.Time `json:"expira"`
	OrgID   string    `json:"org_id"`
	Rol     string    `json:"rol"`
	Usuario string    `json:"usuario_id"`
}

func (s *Servidor) iniciarSesion(w http.ResponseWriter, r *http.Request) {
	var p peticionSesion
	if !leer(w, r, &p) {
		return
	}

	datos, err := s.verif.VerificarGoogle(r.Context(), p.IDTokenGoogle)
	if err != nil {
		http.Error(w, "credencial de Google invalida", http.StatusUnauthorized)
		return
	}

	// PENDIENTE: resolver usuario y membresia contra la base. Mientras no exista
	// :feature:equipo del lado de la app, no hay flujo que cree organizaciones.
	_ = datos
	http.Error(w, "alta de usuarios todavia no implementada", http.StatusNotImplemented)
}

// --- Dispositivos ---------------------------------------------------------------

type peticionDispositivo struct {
	DeviceID string `json:"device_id"`
	FCMToken string `json:"fcm_token"`
}

func (s *Servidor) registrarDispositivo(w http.ResponseWriter, r *http.Request) {
	id, _ := auth.De(r.Context())
	var p peticionDispositivo
	if !leer(w, r, &p) {
		return
	}
	if p.DeviceID == "" {
		http.Error(w, "falta device_id", http.StatusBadRequest)
		return
	}

	var token *string
	if p.FCMToken != "" {
		token = &p.FCMToken
	}

	err := s.alm.RegistrarDispositivo(r.Context(), almacen.Dispositivo{
		ID:        uuid.NewString(),
		OrgID:     id.OrgID,
		UsuarioID: id.UsuarioID,
		DeviceID:  p.DeviceID,
		FCMToken:  token,
	})
	if err != nil {
		s.fallo(w, "registrando dispositivo", err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

type peticionCaja struct {
	DeviceID string `json:"device_id"`
	Abierta  bool   `json:"abierta"`
}

func (s *Servidor) fijarCajaAbierta(w http.ResponseWriter, r *http.Request) {
	id, _ := auth.De(r.Context())
	var p peticionCaja
	if !leer(w, r, &p) {
		return
	}
	if err := s.alm.FijarCajaAbierta(r.Context(), id.OrgID, p.DeviceID, p.Abierta); err != nil {
		s.fallo(w, "fijando estado de caja", err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// --- Pagos ------------------------------------------------------------------------

type peticionPago struct {
	DedupKey      string  `json:"dedup_key"`
	Wallet        string  `json:"wallet"`
	MontoCentavos int64   `json:"monto_centavos"`
	Moneda        string  `json:"moneda"`
	Pagador       *string `json:"pagador"`
	Referencia    *string `json:"referencia"`
	AvisoEnMillis int64   `json:"aviso_en_millis"`
	Nivel         string  `json:"nivel"`
	Confianza     string  `json:"confianza"`
	TurnoID       *string `json:"turno_id"`
	DeviceID      string  `json:"device_id"`
}

type respuestaPago struct {
	// Falso si el pago ya existia. El celular lo marca como sincronizado igual:
	// que ya estuviera es exito, no error.
	Nuevo bool `json:"nuevo"`
}

// subirPago recibe un pago del celular del dueno y lo reenvia a los cajeros.
//
// Es idempotente por (org_id, dedup_key): el celular puede reintentar una subida
// que en realidad si llego, que es lo que pasa cuando se corta la red a mitad de
// la peticion. Sin eso, el modo offline duplicaria pagos.
func (s *Servidor) subirPago(w http.ResponseWriter, r *http.Request) {
	id, _ := auth.De(r.Context())
	var p peticionPago
	if !leer(w, r, &p) {
		return
	}
	if p.DedupKey == "" || p.MontoCentavos <= 0 || p.Wallet == "" {
		http.Error(w, "pago incompleto", http.StatusBadRequest)
		return
	}
	if p.Moneda == "" {
		p.Moneda = "BOB"
	}

	nuevo, err := s.alm.GuardarPago(r.Context(), almacen.Pago{
		ID:              uuid.NewString(),
		OrgID:           id.OrgID,
		DedupKey:        p.DedupKey,
		Wallet:          p.Wallet,
		MontoCentavos:   p.MontoCentavos,
		Moneda:          p.Moneda,
		Pagador:         p.Pagador,
		Referencia:      p.Referencia,
		AvisoEn:         time.UnixMilli(p.AvisoEnMillis).UTC(),
		Nivel:           p.Nivel,
		Confianza:       p.Confianza,
		TurnoID:         p.TurnoID,
		CajeroID:        &id.UsuarioID,
		SubidoPorDevice: p.DeviceID,
	})
	if err != nil {
		s.fallo(w, "guardando pago", err)
		return
	}

	// Solo se reenvia si el pago era nuevo: si no, los cajeros oirian el mismo
	// cobro dos veces por un reintento de red.
	if nuevo {
		s.reenviar(r, id.OrgID, p)
	}

	responder(w, http.StatusOK, respuestaPago{Nuevo: nuevo})
}

func (s *Servidor) reenviar(r *http.Request, orgID string, p peticionPago) {
	tokens, err := s.alm.TokensParaReenvio(r.Context(), orgID, p.DeviceID)
	if err != nil {
		s.log.Error("no se pudieron leer los tokens de reenvio", "error", err)
		return
	}
	if len(tokens) == 0 {
		return
	}

	// Mensaje de datos, sin bloque de notificacion: la app tiene que procesarlo
	// ella misma para meterlo en Room y anunciarlo por voz. Un push de
	// notificacion lo mostraria el sistema sin que la app se entere.
	datos := map[string]string{
		"tipo":           "pago",
		"dedup_key":      p.DedupKey,
		"wallet":         p.Wallet,
		"monto_centavos": strconv.FormatInt(p.MontoCentavos, 10),
		"aviso_en":       strconv.FormatInt(p.AvisoEnMillis, 10),
	}
	if p.Pagador != nil {
		datos["pagador"] = *p.Pagador
	}
	if p.Referencia != nil {
		datos["referencia"] = *p.Referencia
	}

	if err := s.enviador.Enviar(r.Context(), tokens, datos); err != nil {
		// No se falla la peticion: el pago ya quedo guardado. Que el reenvio
		// falle es peor servicio, no perdida de datos.
		s.log.Error("fallo el reenvio por push", "error", err, "destinos", len(tokens))
	}
}

// --- Plantillas ---------------------------------------------------------------------

func (s *Servidor) plantillas(w http.ResponseWriter, r *http.Request) {
	desde := 0
	if v := r.URL.Query().Get("desde"); v != "" {
		n, err := strconv.Atoi(v)
		if err != nil || n < 0 {
			http.Error(w, "parametro 'desde' invalido", http.StatusBadRequest)
			return
		}
		desde = n
	}

	paquete, err := s.alm.UltimasPlantillas(r.Context(), desde)
	if errors.Is(err, almacen.ErrNoEncontrado) {
		// El celular ya esta al dia. 204 y no 404: no es un error.
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if err != nil {
		s.fallo(w, "leyendo plantillas", err)
		return
	}

	// Se devuelve el contenido tal cual se guardo, sin volver a serializarlo: la
	// firma se calculo sobre la forma canonica de ese contenido exacto, y
	// cualquier reserializacion es un riesgo innecesario.
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(`{"bundle":`))
	_, _ = w.Write(paquete.ContenidoRaw)
	_, _ = w.Write([]byte(`,"firma_base64":`))
	_ = json.NewEncoder(w).Encode(paquete.FirmaBase64)
	_, _ = w.Write([]byte(`}`))
}

// --- Reportes -------------------------------------------------------------------------

func (s *Servidor) reporteDelDia(w http.ResponseWriter, r *http.Request) {
	id, _ := auth.De(r.Context())
	if !id.PuedeVerCuadreCompleto() {
		http.Error(w, "el cajero solo ve su propio turno", http.StatusForbidden)
		return
	}

	fecha := r.URL.Query().Get("fecha")
	dia, err := time.Parse("2006-01-02", fecha)
	if err != nil {
		http.Error(w, "fecha invalida, se espera AAAA-MM-DD", http.StatusBadRequest)
		return
	}

	// El dia es el dia de Bolivia, no el dia UTC: un pago de las 21:00 en La Paz
	// pertenece a ese dia comercial y no al siguiente.
	zona, err := time.LoadLocation("America/La_Paz")
	if err != nil {
		zona = time.FixedZone("BOT", -4*60*60)
	}
	desde := time.Date(dia.Year(), dia.Month(), dia.Day(), 0, 0, 0, 0, zona)
	hasta := desde.AddDate(0, 0, 1)

	pagos, err := s.alm.PagosDelDia(r.Context(), id.OrgID, desde, hasta)
	if err != nil {
		s.fallo(w, "leyendo el reporte", err)
		return
	}

	var total int64
	for _, p := range pagos {
		total += p.MontoCentavos
	}
	responder(w, http.StatusOK, map[string]any{
		"fecha":          fecha,
		"cantidad":       len(pagos),
		"total_centavos": total,
		"pagos":          pagos,
	})
}

// --- Avisos no reconocidos ---------------------------------------------------------------

type peticionAviso struct {
	SourcePackage string  `json:"source_package"`
	Titulo        *string `json:"titulo"`
	Texto         *string `json:"texto"`
}

func (s *Servidor) avisoNoReconocido(w http.ResponseWriter, r *http.Request) {
	id, _ := auth.De(r.Context())
	var p peticionAviso
	if !leer(w, r, &p) {
		return
	}
	if p.SourcePackage == "" {
		http.Error(w, "falta source_package", http.StatusBadRequest)
		return
	}

	err := s.alm.GuardarAvisoNoReconocido(
		r.Context(), uuid.NewString(), id.OrgID, p.SourcePackage, p.Titulo, p.Texto,
	)
	if err != nil {
		s.fallo(w, "guardando aviso no reconocido", err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// --- Utilidades -------------------------------------------------------------------------

const maxCuerpo = 1 << 20 // 1 MiB

func leer(w http.ResponseWriter, r *http.Request, destino any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, maxCuerpo)
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(destino); err != nil {
		http.Error(w, "cuerpo invalido", http.StatusBadRequest)
		return false
	}
	return true
}

func responder(w http.ResponseWriter, codigo int, cuerpo any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(codigo)
	_ = json.NewEncoder(w).Encode(cuerpo)
}

// fallo registra el detalle y devuelve un mensaje generico: el error de la base
// puede filtrar nombres de columnas o datos, y al celular no le sirve de nada.
func (s *Servidor) fallo(w http.ResponseWriter, contexto string, err error) {
	s.log.Error(contexto, "error", err)
	http.Error(w, "error interno", http.StatusInternalServerError)
}
