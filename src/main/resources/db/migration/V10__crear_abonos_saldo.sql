-- =====================================================================
-- Abonos de saldo
--
-- El saldo de una cuenta solo aumenta cuando un EJECUTIVO le abona; las
-- compras lo descuentan. Cada abono queda registrado con el ejecutivo que
-- lo hizo y el saldo antes y despues, para poder auditarlo.
-- =====================================================================

CREATE TABLE onboarding.abonos_saldo (
    id               UUID          PRIMARY KEY,
    cuenta_id        INTEGER       NOT NULL REFERENCES onboarding.cuentas (id),
    monto            NUMERIC(15,2) NOT NULL,
    saldo_anterior   NUMERIC(15,2) NOT NULL,
    saldo_nuevo      NUMERIC(15,2) NOT NULL,
    concepto         VARCHAR(100),
    ejecutivo        VARCHAR(100)  NOT NULL,
    fecha_creacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_abonos_monto CHECK (monto > 0),
    CONSTRAINT ck_abonos_saldo CHECK (saldo_nuevo = saldo_anterior + monto)
);

CREATE INDEX ix_abonos_saldo_cuenta ON onboarding.abonos_saldo (cuenta_id, fecha_creacion DESC);
