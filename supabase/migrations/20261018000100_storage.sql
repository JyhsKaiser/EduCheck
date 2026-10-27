-- Bucket privado para las evidencias de los justificantes.
-- Storage hace cumplir el tamaño y el tipo de archivo aunque alguien salte la app.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'evidencias',
  'evidencias',
  false,
  10485760,                                        -- 10 MB (video corto sin audio, foto comprimida)
  array['image/jpeg', 'image/png', 'video/mp4']
)
on conflict (id) do update
  set public = excluded.public,
      file_size_limit = excluded.file_size_limit,
      allowed_mime_types = excluded.allowed_mime_types;

-- Sin políticas sobre storage.objects: nadie accede con anon/authenticated.
-- Las subidas y descargas se hacen con URLs firmadas que emiten las Edge Functions.
