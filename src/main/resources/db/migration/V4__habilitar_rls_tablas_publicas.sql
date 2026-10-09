-- Supabase publica el esquema "public" en su API REST (PostgREST). Con RLS activo y sin
-- politicas, esa API no puede leer ni escribir estas tablas. La aplicacion se conecta
-- como duena de las tablas, por lo que no se ve afectada.
ALTER TABLE public.gestopago_tokens      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.personas              ENABLE ROW LEVEL SECURITY;
