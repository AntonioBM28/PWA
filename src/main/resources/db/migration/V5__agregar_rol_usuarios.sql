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
