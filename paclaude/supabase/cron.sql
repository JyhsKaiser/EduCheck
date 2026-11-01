-- =====================================================================================
--  Tareas programadas (pg_cron + pg_net). Se ejecuta UNA VEZ, a mano, después de desplegar
--  las funciones (SQL Editor de Supabase). No es una migración porque necesita tus valores.
--
--  Paso 1: reemplaza <PROJECT_REF> y <CRON_SECRET> (el mismo valor que pones en
--          `supabase secrets set CRON_SECRET=...`).
--  pg_cron trabaja en UTC. México no tiene horario de verano: hora local = UTC - 6.
-- =====================================================================================
create extension if not exists pg_cron;
create extension if not exists pg_net;

select vault.create_secret('https://<PROJECT_REF>.supabase.co', 'project_url');
select vault.create_secret('<CRON_SECRET>', 'cron_secret');

-- 05:30 local · calcula los límites de cada grupo para el día (de lunes a viernes)
select cron.schedule('educheck-sincronizar-horarios', '30 11 * * 1-5', $$
  select net.http_post(
    url := (select decrypted_secret from vault.decrypted_secrets where name = 'project_url') || '/functions/v1/sincronizar-horarios',
    headers := jsonb_build_object('Content-Type', 'application/json',
      'x-cron-secret', (select decrypted_secret from vault.decrypted_secrets where name = 'cron_secret')),
    body := '{}'::jsonb,
    timeout_milliseconds := 30000
  );
$$);

-- Cada 5 min de 06:00 a 17:55 local · evalúa ausencias y pérdida de contacto
select cron.schedule('educheck-evaluar-avisos', '*/5 12-23 * * 1-5', $$
  select net.http_post(
    url := (select decrypted_secret from vault.decrypted_secrets where name = 'project_url') || '/functions/v1/evaluar-avisos',
    headers := jsonb_build_object('Content-Type', 'application/json',
      'x-cron-secret', (select decrypted_secret from vault.decrypted_secrets where name = 'cron_secret')),
    body := '{}'::jsonb,
    timeout_milliseconds := 30000
  );
$$);

-- 03:00 local · borra evidencias vencidas o huérfanas y limpia tablas de apoyo
select cron.schedule('educheck-limpiar-archivos', '0 9 * * *', $$
  select net.http_post(
    url := (select decrypted_secret from vault.decrypted_secrets where name = 'project_url') || '/functions/v1/limpiar-archivos',
    headers := jsonb_build_object('Content-Type', 'application/json',
      'x-cron-secret', (select decrypted_secret from vault.decrypted_secrets where name = 'cron_secret')),
    body := '{}'::jsonb,
    timeout_milliseconds := 60000
  );
$$);

-- Para ver o quitar tareas:
--   select * from cron.job;
--   select cron.unschedule('educheck-evaluar-avisos');
