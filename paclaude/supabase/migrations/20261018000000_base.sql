-- =====================================================================================
--  EduCheck · esquema de apoyo del backend (Supabase Postgres)
--  Firestore guarda los datos de negocio. Aquí solo hay ESTADO OPERATIVO del backend:
--  tokens FCM, estado del día, horarios del día, avisos enviados, anti-replay y archivos.
--  Todas las tablas tienen RLS activado SIN políticas: la app no accede nunca directo;
--  solo las Edge Functions con la clave de servicio.
-- =====================================================================================

-- Tokens FCM (uno por dispositivo; un usuario puede tener varios)
create table public.fcm_tokens (
  token           text primary key,
  uid             text not null,
  install_id      text,
  rol             text,
  actualizado_en  timestamptz not null default now()
);
create index fcm_tokens_uid_idx on public.fcm_tokens (uid);

-- Estado del día de cada alumno presente (se crea al registrar la entrada)
create table public.estado_dia (
  alumno_id        text        not null,
  fecha            date        not null,
  alumno_uid       text,
  plantel_id       text        not null,
  grupo_id         text,
  nombre           text,
  tutor_uids       text[]      not null default '{}',
  entrada_en       timestamptz not null,
  salida_en        timestamptz,
  ultimo_contacto  timestamptz not null default now(),
  primary key (alumno_id, fecha)
);
create index estado_dia_uid_idx on public.estado_dia (alumno_uid, fecha);
create index estado_dia_abiertos_idx on public.estado_dia (fecha) where salida_en is null;

-- Límites del día por grupo (primera hora de inicio y última de fin), calculados desde Firestore cada mañana
create table public.horarios_dia (
  fecha                 date        not null,
  grupo_id              text        not null,
  plantel_id            text        not null,
  inicio                time        not null,
  fin                   time        not null,
  tolerancia_min        integer     not null default 60,
  zona_horaria          text        not null default 'America/Mexico_City',
  ausencia_evaluada_en  timestamptz,
  primary key (fecha, grupo_id)
);

-- Idempotencia de avisos y eventos: la clave identifica "qué aviso, de quién y de cuándo"
create table public.avisos_enviados (
  clave      text primary key,
  tipo       text        not null,
  creado_en  timestamptz not null default now()
);

-- Anti-replay del QR dinámico (TOTP): un código no puede usarse dos veces
create table public.qr_usados (
  alumno_id  text        not null,
  contador   bigint      not null,
  usado_en   timestamptz not null default now(),
  primary key (alumno_id, contador)
);

-- Metadatos de las evidencias subidas a Storage (foto o video de justificantes)
create table public.archivos (
  path             text primary key,                 -- {uid}/{uuid}.jpg|mp4 dentro del bucket «evidencias»
  owner_uid        text        not null,
  tipo             text        not null check (tipo in ('IMAGEN', 'VIDEO')),
  bytes_declarados integer     not null check (bytes_declarados > 0),
  creado_en        timestamptz not null default now(),
  confirmado_en    timestamptz,                      -- cuando un justificante la referencia
  justificante_id  text
);
create index archivos_owner_idx on public.archivos (owner_uid, creado_en);
create index archivos_pendientes_idx on public.archivos (creado_en) where confirmado_en is null;

-- RLS cerrado: sin políticas, ni anon ni authenticated pueden leer o escribir
alter table public.fcm_tokens      enable row level security;
alter table public.estado_dia      enable row level security;
alter table public.horarios_dia    enable row level security;
alter table public.avisos_enviados enable row level security;
alter table public.qr_usados       enable row level security;
alter table public.archivos        enable row level security;

-- Alumnos presentes, dentro del horario de clases, sin señal por más que la tolerancia del grupo
-- y sin aviso previo. p_hora_local es la hora local del plantel.
create or replace function public.candidatos_sin_contacto(
  p_fecha date,
  p_ahora timestamptz,
  p_hora_local time
)
returns table (
  alumno_id text,
  nombre text,
  tutor_uids text[],
  plantel_id text,
  grupo_id text
)
language sql
stable
as $$
  select e.alumno_id, e.nombre, e.tutor_uids, e.plantel_id, e.grupo_id
  from public.estado_dia e
  join public.horarios_dia h
    on h.fecha = e.fecha and h.grupo_id = e.grupo_id
  where e.fecha = p_fecha
    and e.salida_en is null
    and p_hora_local between h.inicio and h.fin
    and e.ultimo_contacto < p_ahora - make_interval(mins => h.tolerancia_min)
    and not exists (
      select 1 from public.avisos_enviados a
      where a.clave = 'SINCONTACTO:' || e.alumno_id || ':' || e.fecha::text
    );
$$;

-- Solo la clave de servicio (Edge Functions) puede ejecutarla
revoke all on function public.candidatos_sin_contacto(date, timestamptz, time) from public, anon, authenticated;
grant execute on function public.candidatos_sin_contacto(date, timestamptz, time) to service_role;
