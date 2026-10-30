CREATE TABLE onboarding.transacciones (
    id UUID PRIMARY KEY,
    cliente_id INTEGER NOT NULL REFERENCES onboarding.clientes(id) ON DELETE RESTRICT,
    cuenta_id INTEGER NOT NULL REFERENCES onboarding.cuentas(id) ON DELETE RESTRICT,
    clave_idempotencia VARCHAR(64) NOT NULL,
    huella_solicitud CHAR(64) NOT NULL,
    upc VARCHAR(15) NOT NULL,
    payload_cifrado TEXT NOT NULL,
    random_proveedor VARCHAR(32) NOT NULL,
    monto NUMERIC(15,2) NOT NULL CHECK (monto > 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'CONCILIACION', 'APROBADA', 'RECHAZADA')),
    codigo_proveedor VARCHAR(8),
    mensaje_proveedor VARCHAR(500),
    id_tx VARCHAR(32),
    numero_autorizacion VARCHAR(60),
    pin VARCHAR(250),
    folio VARCHAR(250),
    leyenda VARCHAR(5000),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_transacciones_cliente_idempotencia UNIQUE (cliente_id, clave_idempotencia),
    CONSTRAINT uq_transacciones_upc UNIQUE (upc)
);

CREATE INDEX ix_transacciones_estado_fecha ON onboarding.transacciones (estado, fecha_creacion);
