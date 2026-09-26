-- +goose Up

-- Un comercio. Todo cuelga de aqui.
CREATE TABLE orgs (
    id          UUID PRIMARY KEY,
    nombre      TEXT        NOT NULL,
    creada_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Una persona, identificada por su cuenta de Google.
CREATE TABLE usuarios (
    id            UUID PRIMARY KEY,
    google_sub    TEXT        NOT NULL UNIQUE,
    email         TEXT        NOT NULL,
    nombre        TEXT        NOT NULL,
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Que rol tiene cada persona en cada comercio.
CREATE TABLE membresias (
    org_id      UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    usuario_id  UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol         TEXT        NOT NULL CHECK (rol IN ('dueno', 'encargado', 'cajero')),
    activa      BOOLEAN     NOT NULL DEFAULT true,
    creada_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (org_id, usuario_id)
);

-- Un celular. El token de FCM vive aqui, no en el usuario: una persona puede
-- tener dos celulares, y el reenvio de pagos va por dispositivo.
CREATE TABLE dispositivos (
    id            UUID PRIMARY KEY,
    org_id        UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    usuario_id    UUID        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    device_id     TEXT        NOT NULL,
    fcm_token     TEXT,
    -- Solo se le reenvian pagos a un dispositivo con la caja abierta: mandarle
    -- push a un celular guardado en un cajon gasta bateria y no sirve a nadie.
    caja_abierta  BOOLEAN     NOT NULL DEFAULT false,
    visto_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, device_id)
);

CREATE INDEX dispositivos_para_reenvio ON dispositivos (org_id) WHERE caja_abierta;

CREATE TABLE turnos (
    id            UUID PRIMARY KEY,
    org_id        UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    usuario_id    UUID        NOT NULL REFERENCES usuarios(id),
    device_id     TEXT        NOT NULL,
    abierto_en    TIMESTAMPTZ NOT NULL,
    cerrado_en    TIMESTAMPTZ
);

CREATE INDEX turnos_abiertos ON turnos (org_id) WHERE cerrado_en IS NULL;

-- Los pagos.
--
-- dedup_key es la misma que calcula la app, y el UNIQUE de aqui es la segunda
-- linea de defensa: si el celular reintenta una subida que en realidad si
-- llego, el pago no se duplica del lado del servidor. La subida puede entonces
-- reintentarse sin miedo, que es lo que hace posible el modo offline.
CREATE TABLE pagos (
    id                  UUID PRIMARY KEY,
    org_id              UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    dedup_key           TEXT        NOT NULL,
    wallet              TEXT        NOT NULL,
    -- Centavos enteros. Nunca NUMERIC con decimales ni punto flotante.
    monto_centavos      BIGINT      NOT NULL CHECK (monto_centavos > 0),
    moneda              TEXT        NOT NULL DEFAULT 'BOB',
    pagador             TEXT,
    referencia          TEXT,
    aviso_en            TIMESTAMPTZ NOT NULL,
    nivel               TEXT        NOT NULL CHECK (nivel IN ('aviso_banco', 'no_llego', 'confirmado_api')),
    confianza           TEXT        NOT NULL CHECK (confianza IN ('completa', 'parcial')),
    turno_id            UUID        REFERENCES turnos(id),
    cajero_id           UUID        REFERENCES usuarios(id),
    subido_por_device   TEXT        NOT NULL,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, dedup_key)
);

CREATE INDEX pagos_por_fecha ON pagos (org_id, aviso_en DESC);
CREATE INDEX pagos_por_turno ON pagos (turno_id);

-- Paquetes de plantillas firmados. Se guarda la firma junto al contenido para
-- poder reenviar exactamente lo mismo que se firmo.
CREATE TABLE plantillas (
    version       INTEGER     PRIMARY KEY,
    contenido     JSONB       NOT NULL,
    canonico      TEXT        NOT NULL,
    firma_base64  TEXT        NOT NULL,
    publicada_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Invitaciones por QR. Cortas y de un solo uso.
CREATE TABLE invitaciones (
    id          UUID PRIMARY KEY,
    org_id      UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    rol         TEXT        NOT NULL CHECK (rol IN ('encargado', 'cajero')),
    token_hash  TEXT        NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,
    usada_en    TIMESTAMPTZ,
    creada_por  UUID        NOT NULL REFERENCES usuarios(id),
    creada_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Avisos que ninguna plantilla reconocio, subidos con consentimiento explicito.
-- Es lo que permite corregir una plantilla cuando un banco cambia de formato.
CREATE TABLE avisos_no_reconocidos (
    id              UUID PRIMARY KEY,
    org_id          UUID        NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
    source_package  TEXT        NOT NULL,
    titulo          TEXT,
    texto           TEXT,
    recibido_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX avisos_por_paquete ON avisos_no_reconocidos (source_package, recibido_en DESC);

-- +goose Down
DROP TABLE IF EXISTS avisos_no_reconocidos;
DROP TABLE IF EXISTS invitaciones;
DROP TABLE IF EXISTS plantillas;
DROP TABLE IF EXISTS pagos;
DROP TABLE IF EXISTS turnos;
DROP TABLE IF EXISTS dispositivos;
DROP TABLE IF EXISTS membresias;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS orgs;
