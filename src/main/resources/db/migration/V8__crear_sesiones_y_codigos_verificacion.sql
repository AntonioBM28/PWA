-- =====================================================================
-- Sesiones respaldadas por el servidor y codigos de verificacion (2FA)
--
--  * sesiones: cada JWT lleva el id de una fila (claim jti). Una peticion solo
--    se acepta si su sesion sigue abierta y vigente, asi el servidor puede
--    cerrar una sesion antes de que el token expire (logout, o cuando el
--    usuario inicia sesion en otro dispositivo). Un usuario tiene como maximo
--    una sesion abierta.
--  * codigos_verificacion: segundo factor enviado por correo. Se guarda solo
--    el hash BCrypt del codigo, con vigencia, intentos y reenvios limitados.
-- =====================================================================

CREATE TABLE onboarding.sesiones (
    id                 UUID         PRIMARY KEY,
    usuario_id         INTEGER      NOT NULL REFERENCES onboarding.usuarios (id),
    plataforma         VARCHAR(5)   NOT NULL,
    dispositivo        VARCHAR(64),
    fecha_creacion     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_expiracion   TIMESTAMPTZ  NOT NULL,
    fecha_cierre       TIMESTAMPTZ,
    motivo_cierre      VARCHAR(12),
    CONSTRAINT ck_sesiones_plataforma CHECK (plataforma IN ('WEB', 'MOVIL')),
    CONSTRAINT ck_sesiones_motivo CHECK (motivo_cierre IN ('LOGOUT', 'REEMPLAZADA')),
    CONSTRAINT ck_sesiones_cierre CHECK ((fecha_cierre IS NULL) = (motivo_cierre IS NULL))
);

-- Busqueda de la sesion abierta de un usuario (login y cada peticion)
CREATE INDEX ix_sesiones_usuario_abiertas
    ON onboarding.sesiones (usuario_id) WHERE fecha_cierre IS NULL;

CREATE TABLE onboarding.codigos_verificacion (
    id                      UUID         PRIMARY KEY,
    usuario_id              INTEGER      NOT NULL REFERENCES onboarding.usuarios (id),
    codigo_hash             VARCHAR(60)  NOT NULL,
    plataforma              VARCHAR(5)   NOT NULL,
    dispositivo             VARCHAR(64),
    cerrar_sesion_anterior  BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos_fallidos       SMALLINT     NOT NULL DEFAULT 0,
    reenvios                SMALLINT     NOT NULL DEFAULT 0,
    fecha_creacion          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_envio             TIMESTAMPTZ  NOT NULL,
    fecha_expiracion        TIMESTAMPTZ  NOT NULL,
    fecha_uso               TIMESTAMPTZ,
    CONSTRAINT ck_codigos_plataforma CHECK (plataforma IN ('WEB', 'MOVIL')),
    CONSTRAINT ck_codigos_intentos CHECK (intentos_fallidos >= 0 AND reenvios >= 0)
);

CREATE INDEX ix_codigos_verificacion_usuario ON onboarding.codigos_verificacion (usuario_id);
