# EduCheck · Notificaciones push: puesta en marcha (Sprint 5)

Se hace una sola vez. Necesitas el proyecto de Supabase (ya creado) y la cuenta de servicio de Firebase (ya guardada como secreto).

## 0. Requisito en Google Cloud
En la consola de Google Cloud del proyecto de Firebase, la API **Firebase Cloud Messaging API (V1)** debe estar habilitada (viene habilitada en proyectos nuevos). Está en *APIs y servicios → API habilitadas*. La cuenta de servicio de Firebase ya tiene permiso para enviar mensajes.

## 1. Tabla del cursor (Supabase)
Opción A, con la CLI desde la raíz del repositorio:
```
npx supabase db push
```
Opción B, sin CLI: Dashboard de Supabase → **SQL Editor** → pega el contenido de `supabase/migrations/20261101000000_push_state.sql` → Run.

## 2. Secreto del cron
Genera una cadena aleatoria larga (PowerShell):
```
-join ((48..57)+(97..122) | Get-Random -Count 40 | ForEach-Object {[char]$_})
```
y guárdala como secreto (cámbiala por la tuya):
```
npx supabase secrets set CRON_SECRET=<la-cadena>
```

## 3. Desplegar las funciones
Agrega al `supabase/config.toml` lo de `supabase/config.toml.snippet` y despliega (las funciones compartidas `_shared` se incluyen solas):
```
npx supabase functions deploy dispatch-notifications --no-verify-jwt
npx supabase functions deploy send-push --no-verify-jwt
```
Las demás funciones usan el mismo `_shared`, pero no hace falta volver a desplegarlas ahora. Si más adelante cambias `create-user`, `set-role` o `set-user-active`, redespliégalas para que tomen el `_shared` nuevo.

## 4. Programar el cron (cada minuto)
Dashboard → **SQL Editor**. Abre `supabase/cron/schedule.sql.example`, reemplaza `<PROJECT_REF>` (`acrnuueamztpukdeskse`) y `<CRON_SECRET>` (la cadena del paso 2) y ejecútalo. **No subas ese archivo con el secreto real al repositorio.**

Para verificar que corre: `select * from cron.job_run_details order by start_time desc limit 10;` (estado `succeeded`) y los logs de la función en Dashboard → Edge Functions → dispatch-notifications → Logs.

## 5. Probar
1. **Aplicación:** copia los archivos de `main/` y compila. Inicia sesión como **tutor** en un teléfono o emulador con Google Play. Acepta el permiso de notificaciones. En Firestore, `devices` debe tener un documento `{installId}_{uid}` con `role = GUARDIAN`, `status = ACTIVE` y `fcmToken` lleno.
2. **Prueba directa** (sustituye el token de un ADMIN y el uid del tutor; en Windows usa `curl.exe`):
   ```
   curl -s -X POST https://acrnuueamztpukdeskse.supabase.co/functions/v1/send-push -H "Authorization: Bearer <idToken-de-un-ADMIN>" -H "Content-Type: application/json" -d '{"uid":"<uid-del-tutor>"}'
   ```
   Debe responder `{"ok":true,"sent":1,...}` y el teléfono muestra "Prueba de notificación". Si responde `no_active_device_with_token`, el tutor aún no registró su token.
3. **Aviso de ingreso:** vincula al tutor con el alumno (Gestión → Alumnos → editar → Tutores) **antes** de que el alumno registre su entrada. Registra la entrada del alumno en otro dispositivo. En menos de un minuto el tutor recibe "Entrada registrada · <Alumno> llegó al plantel a las HH:mm".
4. **Ejecutarlo a mano** (sin esperar al cron): `POST /dispatch-notifications` con el token de un ADMIN. Responde `{"ok":true,"presences":N,"notified":N,"skipped":N,"pushes":N}`.

## Si algo no llega
| Síntoma | Qué revisar |
|---|---|
| `push_state read failed` en los logs | No se aplicó la migración del paso 1. |
| 401/403 del cron | El `CRON_SECRET` del SQL no coincide con el secreto de Supabase. |
| `notified > 0` pero `pushes = 0` | El tutor no tiene dispositivo `ACTIVE` con token (revisa `devices`), o inició sesión y cerró sesión (el token queda en blanco). |
| `skipped` alto y no llega nada | Esa entrada ya se avisó antes (el aviso se envía una sola vez por tutor y entrada). |
| No llega en segundo plano | Algunos fabricantes (Xiaomi, Huawei...) frenan las apps en segundo plano: quita la optimización de batería de EduCheck. |
