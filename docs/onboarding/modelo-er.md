# Onboarding de clientes – Modelo entidad-relación

Esquema `onboarding` (PostgreSQL / Supabase). Script: [V3__crear_esquema_onboarding.sql](../../src/main/resources/db/migration/V3__crear_esquema_onboarding.sql) (migración Flyway).

```mermaid
erDiagram
    CLIENTES ||--|| DOMICILIOS : "tiene"
    CLIENTES ||--|| USUARIOS   : "accede con"
    CLIENTES ||--o{ CUENTAS    : "posee"

    CLIENTES {
        INTEGER       id PK
        VARCHAR_50    nombre
        VARCHAR_50    segundo_nombre "nullable"
        VARCHAR_50    apellido_paterno
        VARCHAR_50    apellido_materno
        DATE          fecha_nacimiento
        VARCHAR_18    curp UK
        VARCHAR_13    rfc UK "13 caracteres"
        VARCHAR_1     sexo "H | M"
        VARCHAR_3     nacionalidad "ISO 3166 (MEX)"
        VARCHAR_12    estado_civil
        VARCHAR_100   correo UK
        VARCHAR_10    telefono_movil
        VARCHAR_10    telefono_alternativo "nullable"
        VARCHAR_60    ocupacion
        VARCHAR_100   empresa
        NUMERIC_12_2  ingreso_mensual "> 0"
        BOOLEAN       activo
        TIMESTAMPTZ   fecha_creacion
        TIMESTAMPTZ   fecha_actualizacion
    }

    DOMICILIOS {
        INTEGER      id PK
        INTEGER      cliente_id FK,UK
        VARCHAR_100  calle
        VARCHAR_10   numero_exterior
        VARCHAR_10   numero_interior "nullable"
        VARCHAR_100  colonia
        VARCHAR_100  municipio
        VARCHAR_50   estado
        VARCHAR_5    codigo_postal
        VARCHAR_3    pais "fijo MEX"
        TIMESTAMPTZ  fecha_creacion
        TIMESTAMPTZ  fecha_actualizacion
    }

    CUENTAS {
        INTEGER       id PK
        INTEGER       cliente_id FK
        VARCHAR_10    numero_cuenta UK "secuencia"
        VARCHAR_10    estatus "ACTIVA por defecto"
        NUMERIC_15_2  saldo ">= 0"
        TIMESTAMPTZ   fecha_creacion
        TIMESTAMPTZ   fecha_actualizacion
    }

    USUARIOS {
        INTEGER      id PK
        INTEGER      cliente_id FK,UK "nulo solo para EJECUTIVO"
        VARCHAR_10   rol "CLIENTE | EJECUTIVO"
        VARCHAR_100  correo UK "FK compuesta con clientes"
        VARCHAR_60   password "hash BCrypt"
        BOOLEAN      activo
        TIMESTAMPTZ  fecha_creacion
        TIMESTAMPTZ  fecha_actualizacion
    }
```

## Relaciones

| Relación | Cardinalidad | Cómo se garantiza |
|---|---|---|
| Cliente – Domicilio | 1 a 1 | FK `domicilios.cliente_id` + `UNIQUE` |
| Cliente – Usuario | 1 a 1 | FK `usuarios.cliente_id` + `UNIQUE` |
| Cliente – Cuenta | 1 a N | FK `cuentas.cliente_id` (con índice) |

Todas las FK son `ON DELETE RESTRICT`: la baja es lógica (`activo = false`), nunca se borran filas.

## Roles (migración V5)

| Rol | `cliente_id` | Acceso |
|---|---|---|
| `CLIENTE` | obligatorio | Solo sus propios datos y cuentas |
| `EJECUTIVO` | nulo | Administra a todos los clientes, cuentas y usuarios |

`ck_usuarios_rol_cliente` garantiza que un CLIENTE siempre tenga cliente y un EJECUTIVO nunca.
