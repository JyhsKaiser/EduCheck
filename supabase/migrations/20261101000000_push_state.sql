-- Cursor of the notification dispatcher (Sprint 5). Only the Edge Functions (service role) use it.
create table if not exists public.push_state (
  name        text primary key,
  cursor      timestamptz not null,
  updated_at  timestamptz not null default now()
);

-- Row level security on with no policies: the app (anon / authenticated roles) cannot read or write it.
-- The service role used by the Edge Functions bypasses it.
alter table public.push_state enable row level security;
