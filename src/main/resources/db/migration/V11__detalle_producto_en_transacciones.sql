-- =====================================================================
-- Historial de operaciones
--
-- Cada transaccion guarda que producto se compro (clave, nombre y servicio)
-- y la referencia enmascarada (telefono o numero de recibo), para mostrar el
-- historial de compras del cliente y la bitacora de operaciones del
-- ejecutivo sin depender del catalogo vigente ni descifrar el payload.
-- Las transacciones anteriores quedan con estas columnas en NULL.
-- =====================================================================

ALTER TABLE onboarding.transacciones
    ADD COLUMN id_producto INTEGER,
    ADD COLUMN id_servicio INTEGER,
    ADD COLUMN producto    VARCHAR(200),
    ADD COLUMN servicio    VARCHAR(150),
    ADD COLUMN referencia  VARCHAR(20);

CREATE INDEX IF NOT EXISTS ix_transacciones_cliente_fecha
    ON onboarding.transacciones (cliente_id, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS ix_transacciones_fecha
    ON onboarding.transacciones (fecha_creacion DESC);
