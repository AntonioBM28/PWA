-- Script consolidado de la base de datos de la aplicación.
-- PostgreSQL 15+ / Supabase. Ejecutar solo sobre una base nueva.
-- En despliegues normales, Flyway aplica estos archivos en orden: V1 a V5.
-- No ejecutar en una base donde Flyway ya haya aplicado las migraciones.


-- ---------------------------------------------------------------------
-- Migración: src/main/resources/db/migration/V1__create_gestopago_tokens.sql
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS gestopago_tokens (
    id                  SERIAL PRIMARY KEY,
    id_distribuidor     INTEGER         NOT NULL,
    codigo_dispositivo  VARCHAR(100)    NOT NULL,
    token               TEXT            NOT NULL,
    token_type          VARCHAR(50),
    expires_in          BIGINT,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP       NOT NULL DEFAULT NOW(),
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
);

-- ---------------------------------------------------------------------
-- Migración: src/main/resources/db/migration/V2__create_personas.sql
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS personas (
    id               SERIAL PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100),
    apellido_materno VARCHAR(100)
);

-- ---------------------------------------------------------------------
-- Migración: src/main/resources/db/migration/V3__crear_esquema_onboarding.sql
-- ---------------------------------------------------------------------

-- =====================================================================
-- Onboarding de clientes personas fisicas - Script de base de datos
-- PostgreSQL 15+ (Supabase)
--
-- Notas de diseno:
--  * Esquema propio "onboarding": Supabase publica el esquema "public" en su
--    API REST (PostgREST); estas tablas no deben quedar expuestas ahi.
--  * Columnas ordenadas de mayor a menor alineacion (8 bytes -> 4 -> 1 ->
--    longitud variable) para evitar bytes de relleno en cada fila.
--  * En PostgreSQL CHAR(n) no ahorra espacio frente a VARCHAR(n) (rellena con
--    espacios), por eso los campos de longitud fija usan VARCHAR(n) + CHECK.
--  * Importes en NUMERIC (exacto); nunca REAL/DOUBLE para dinero.
--  * Correos guardados en minusculas (CHECK), asi el UNIQUE no distingue
--    mayusculas sin necesidad de un indice funcional.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS onboarding;

-- ---------------------------------------------------------------------
-- CLIENTES: datos personales, de contacto e informacion laboral
-- ---------------------------------------------------------------------
CREATE TABLE onboarding.clientes (
    fecha_creacion        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    id                    INTEGER       GENERATED ALWAYS AS IDENTITY,
    fecha_nacimiento      DATE          NOT NULL,
    activo                BOOLEAN       NOT NULL DEFAULT TRUE,
    sexo                  VARCHAR(1)    NOT NULL,
    nombre                VARCHAR(50)   NOT NULL,
    segundo_nombre        VARCHAR(50),
    apellido_paterno      VARCHAR(50)   NOT NULL,
    apellido_materno      VARCHAR(50)   NOT NULL,
    curp                  VARCHAR(18)   NOT NULL,
    rfc                   VARCHAR(13)   NOT NULL,
    nacionalidad          VARCHAR(3)    NOT NULL DEFAULT 'MEX',
    estado_civil          VARCHAR(12)   NOT NULL,
    correo                VARCHAR(100)  NOT NULL,
    telefono_movil        VARCHAR(10)   NOT NULL,
    telefono_alternativo  VARCHAR(10),
    ocupacion             VARCHAR(60)   NOT NULL,
    empresa               VARCHAR(100)  NOT NULL,
    ingreso_mensual       NUMERIC(12,2) NOT NULL,

    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uq_clientes_curp   UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc    UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    -- Requerido por la FK compuesta de usuarios (mantiene el correo sincronizado)
    CONSTRAINT uq_clientes_id_correo UNIQUE (id, correo),

    CONSTRAINT ck_clientes_nombre           CHECK (nombre           ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_clientes_segundo_nombre   CHECK (segundo_nombre   ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_clientes_apellido_paterno CHECK (apellido_paterno ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_clientes_apellido_materno CHECK (apellido_materno ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$'),
    CONSTRAINT ck_clientes_curp CHECK (curp ~
        '^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$'),
    CONSTRAINT ck_clientes_rfc CHECK (rfc ~
        '^[A-ZÑ&]{4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{2}[0-9A]$'),
    CONSTRAINT ck_clientes_sexo         CHECK (sexo IN ('H', 'M')),
    CONSTRAINT ck_clientes_nacionalidad CHECK (nacionalidad ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_clientes_estado_civil CHECK (estado_civil IN
        ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE')),
    CONSTRAINT ck_clientes_correo CHECK (correo = lower(correo)
        AND correo ~ '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$'),
    CONSTRAINT ck_clientes_telefono_movil       CHECK (telefono_movil       ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_telefono_alternativo CHECK (telefono_alternativo ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_ingreso_mensual      CHECK (ingreso_mensual > 0),
    -- La mayoria de edad se valida en Java (depende de la fecha actual);
    -- aqui solo se descartan fechas imposibles.
    CONSTRAINT ck_clientes_fecha_nacimiento CHECK (fecha_nacimiento > DATE '1900-01-01')
);

-- Busquedas por nombre/apellidos (prefijo, sin distinguir mayusculas)
CREATE INDEX ix_clientes_nombre           ON onboarding.clientes (lower(nombre)           text_pattern_ops);
CREATE INDEX ix_clientes_apellido_paterno ON onboarding.clientes (lower(apellido_paterno) text_pattern_ops);
CREATE INDEX ix_clientes_apellido_materno ON onboarding.clientes (lower(apellido_materno) text_pattern_ops);
-- Clientes registrados en un rango de fechas
CREATE INDEX ix_clientes_fecha_creacion   ON onboarding.clientes (fecha_creacion);

-- ---------------------------------------------------------------------
-- DOMICILIOS: relacion 1 a 1 con clientes
-- ---------------------------------------------------------------------
CREATE TABLE onboarding.domicilios (
    fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    id                   INTEGER      GENERATED ALWAYS AS IDENTITY,
    cliente_id           INTEGER      NOT NULL,
    calle                VARCHAR(100) NOT NULL,
    numero_exterior      VARCHAR(10)  NOT NULL,
    numero_interior      VARCHAR(10),
    colonia              VARCHAR(100) NOT NULL,
    municipio            VARCHAR(100) NOT NULL,
    estado               VARCHAR(50)  NOT NULL,
    codigo_postal        VARCHAR(5)   NOT NULL,
    pais                 VARCHAR(3)   NOT NULL DEFAULT 'MEX',

    CONSTRAINT pk_domicilios PRIMARY KEY (id),
    -- UNIQUE garantiza la relacion 1 a 1 y sirve como indice de la FK
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id)
        REFERENCES onboarding.clientes (id) ON DELETE RESTRICT,
    CONSTRAINT ck_domicilios_codigo_postal CHECK (codigo_postal ~ '^[0-9]{5}$'),
    -- Solo Mexico: estado, municipio y colonia se validan contra la API de CP (postali.app)
    CONSTRAINT ck_domicilios_pais          CHECK (pais = 'MEX')
);

-- ---------------------------------------------------------------------
-- CUENTAS: relacion N a 1 con clientes
-- ---------------------------------------------------------------------
-- Numeros de cuenta de 10 digitos, unicos y sin reintentos
CREATE SEQUENCE onboarding.seq_numero_cuenta
    AS BIGINT START WITH 1000000000 MINVALUE 1000000000 MAXVALUE 9999999999 NO CYCLE;

CREATE TABLE onboarding.cuentas (
    fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    id                   INTEGER       GENERATED ALWAYS AS IDENTITY,
    cliente_id           INTEGER       NOT NULL,
    numero_cuenta        VARCHAR(10)   NOT NULL
        DEFAULT nextval('onboarding.seq_numero_cuenta')::TEXT,
    estatus              VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA',
    saldo                NUMERIC(15,2) NOT NULL DEFAULT 0,

    CONSTRAINT pk_cuentas PRIMARY KEY (id),
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id)
        REFERENCES onboarding.clientes (id) ON DELETE RESTRICT,
    CONSTRAINT ck_cuentas_numero_cuenta CHECK (numero_cuenta ~ '^[0-9]{10}$'),
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA')),
    CONSTRAINT ck_cuentas_saldo   CHECK (saldo >= 0)
);

-- PostgreSQL no indexa las FK automaticamente
CREATE INDEX ix_cuentas_cliente ON onboarding.cuentas (cliente_id);
CREATE INDEX ix_cuentas_estatus ON onboarding.cuentas (estatus);

-- ---------------------------------------------------------------------
-- USUARIOS: acceso al sistema, relacion 1 a 1 con clientes
-- ---------------------------------------------------------------------
CREATE TABLE onboarding.usuarios (
    fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    id                   INTEGER      GENERATED ALWAYS AS IDENTITY,
    cliente_id           INTEGER      NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    correo               VARCHAR(100) NOT NULL,
    password             VARCHAR(60)  NOT NULL,

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_correo  UNIQUE (correo),
    -- El correo del usuario siempre es el del cliente: si cambia en clientes,
    -- se actualiza aqui en cascada.
    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id, correo)
        REFERENCES onboarding.clientes (id, correo) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- Hash BCrypt: siempre 60 caracteres con prefijo $2a$, $2b$ o $2y$
    CONSTRAINT ck_usuarios_password CHECK (password ~ '^\$2[aby]\$[0-9]{2}\$.{53}$')
);

-- ---------------------------------------------------------------------
-- Migración: src/main/resources/db/migration/V4__habilitar_rls_tablas_publicas.sql
-- ---------------------------------------------------------------------

-- Supabase publica el esquema "public" en su API REST (PostgREST). Con RLS activo y sin
-- politicas, esa API no puede leer ni escribir estas tablas. La aplicacion se conecta
-- como duena de las tablas, por lo que no se ve afectada.
ALTER TABLE public.gestopago_tokens      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.personas              ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------
-- Migración: src/main/resources/db/migration/V5__agregar_rol_usuarios.sql
-- ---------------------------------------------------------------------

-- =====================================================================
-- Roles de acceso
--  * CLIENTE: usuario de un cliente; solo puede consultar y modificar sus
--    propios datos y cuentas.
--  * EJECUTIVO: personal de la institucion; administra a todos los clientes.
--    No es un cliente, por eso no tiene cliente_id.
--
-- La FK compuesta (cliente_id, correo) no se evalua cuando cliente_id es NULL
-- (MATCH SIMPLE), asi que el correo de un ejecutivo no depende de clientes;
-- uq_usuarios_correo sigue impidiendo que se repita.
-- =====================================================================

ALTER TABLE onboarding.usuarios
    ADD COLUMN rol VARCHAR(10) NOT NULL DEFAULT 'CLIENTE';

ALTER TABLE onboarding.usuarios
    ALTER COLUMN cliente_id DROP NOT NULL;

ALTER TABLE onboarding.usuarios
    ADD CONSTRAINT ck_usuarios_rol CHECK (rol IN ('CLIENTE', 'EJECUTIVO')),
    -- Un CLIENTE siempre tiene cliente; un EJECUTIVO nunca
    ADD CONSTRAINT ck_usuarios_rol_cliente CHECK ((rol = 'CLIENTE') = (cliente_id IS NOT NULL));
