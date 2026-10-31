ALTER TABLE onboarding.transacciones
    ALTER COLUMN huella_solicitud TYPE VARCHAR(64)
    USING rtrim(huella_solicitud)::VARCHAR(64);
