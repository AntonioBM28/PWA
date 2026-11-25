-- =====================================================================
-- Informacion laboral opcional y con catalogos
--
--  * ocupacion: deja de ser texto libre; se elige de un catalogo.
--    Las ocupaciones capturadas antes quedan como 'OTRA'.
--  * ingreso_mensual (monto libre) se reemplaza por rango_ingreso_mensual
--    (tabulador). Los montos existentes se convierten a su rango.
--  * ocupacion, empresa y el rango de ingreso son opcionales.
-- =====================================================================

ALTER TABLE onboarding.clientes
    ALTER COLUMN ocupacion DROP NOT NULL,
    ALTER COLUMN empresa   DROP NOT NULL,
    ADD COLUMN rango_ingreso_mensual VARCHAR(20);

UPDATE onboarding.clientes SET ocupacion = 'OTRA'
WHERE ocupacion IS NOT NULL AND ocupacion NOT IN (
    'EMPLEADO_SECTOR_PRIVADO', 'SERVIDOR_PUBLICO', 'PROFESIONISTA_INDEPENDIENTE', 'EMPRESARIO',
    'COMERCIANTE', 'TRABAJADOR_POR_CUENTA_PROPIA', 'OFICIO_TECNICO', 'DOCENTE',
    'PROFESIONAL_DE_LA_SALUD', 'FUERZAS_ARMADAS_SEGURIDAD', 'AGROPECUARIO', 'ESTUDIANTE',
    'HOGAR', 'JUBILADO_PENSIONADO', 'DESEMPLEADO', 'OTRA');

UPDATE onboarding.clientes SET rango_ingreso_mensual = CASE
    WHEN ingreso_mensual <=   5000 THEN 'HASTA_5000'
    WHEN ingreso_mensual <=  10000 THEN 'DE_5001_A_10000'
    WHEN ingreso_mensual <=  20000 THEN 'DE_10001_A_20000'
    WHEN ingreso_mensual <=  35000 THEN 'DE_20001_A_35000'
    WHEN ingreso_mensual <=  50000 THEN 'DE_35001_A_50000'
    WHEN ingreso_mensual <= 100000 THEN 'DE_50001_A_100000'
    ELSE 'MAS_DE_100000'
END;

-- Una empresa vacia se guarda como NULL
UPDATE onboarding.clientes SET empresa = NULL WHERE btrim(empresa) = '';

ALTER TABLE onboarding.clientes
    DROP CONSTRAINT ck_clientes_ingreso_mensual,
    DROP COLUMN ingreso_mensual,
    ADD CONSTRAINT ck_clientes_ocupacion CHECK (ocupacion IN (
        'EMPLEADO_SECTOR_PRIVADO', 'SERVIDOR_PUBLICO', 'PROFESIONISTA_INDEPENDIENTE', 'EMPRESARIO',
        'COMERCIANTE', 'TRABAJADOR_POR_CUENTA_PROPIA', 'OFICIO_TECNICO', 'DOCENTE',
        'PROFESIONAL_DE_LA_SALUD', 'FUERZAS_ARMADAS_SEGURIDAD', 'AGROPECUARIO', 'ESTUDIANTE',
        'HOGAR', 'JUBILADO_PENSIONADO', 'DESEMPLEADO', 'OTRA')),
    ADD CONSTRAINT ck_clientes_rango_ingreso_mensual CHECK (rango_ingreso_mensual IN (
        'HASTA_5000', 'DE_5001_A_10000', 'DE_10001_A_20000', 'DE_20001_A_35000',
        'DE_35001_A_50000', 'DE_50001_A_100000', 'MAS_DE_100000')),
    ADD CONSTRAINT ck_clientes_empresa CHECK (btrim(empresa) <> '');
