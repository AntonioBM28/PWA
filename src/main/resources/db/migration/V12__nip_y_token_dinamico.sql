-- =====================================================================
-- Seguridad de las compras (como una transaccion bancaria)
--
--  * nips_transaccion: NIP de 4 digitos que el CLIENTE configura para
--    autorizar compras. Solo se guarda su hash BCrypt; tras varios intentos
--    fallidos queda bloqueado por un tiempo.
--  * autorizaciones_transaccion: token dinamico de un solo uso que se emite
--    despues de validar el NIP. Vence en segundos y esta ligado a una
--    operacion concreta (huella de cuenta, producto, destino y monto).
-- =====================================================================

CREATE TABLE onboarding.nips_transaccion (
    usuario_id           INTEGER      PRIMARY KEY REFERENCES onboarding.usuarios (id),
    nip_hash             VARCHAR(60)  NOT NULL,
    intentos_fallidos    SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta      TIMESTAMPTZ,
    fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_nips_intentos CHECK (intentos_fallidos >= 0)
);

CREATE TABLE onboarding.autorizaciones_transaccion (
    id                  UUID         PRIMARY KEY,
    usuario_id          INTEGER      NOT NULL REFERENCES onboarding.usuarios (id),
    huella_solicitud    VARCHAR(64)  NOT NULL,
    token_hash          VARCHAR(60)  NOT NULL,
    intentos_fallidos   SMALLINT     NOT NULL DEFAULT 0,
    clave_idempotencia  VARCHAR(64),
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_expiracion    TIMESTAMPTZ  NOT NULL,
    fecha_uso           TIMESTAMPTZ,
    CONSTRAINT ck_autorizaciones_intentos CHECK (intentos_fallidos >= 0)
);

CREATE INDEX ix_autorizaciones_usuario ON onboarding.autorizaciones_transaccion (usuario_id, fecha_creacion DESC);
