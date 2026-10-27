# EduCheck · Configuración inicial (etapa 0)

Esta etapa deja el proyecto listo para desarrollar: Hilt, Room, WorkManager, DataStore, Firebase
(Authentication, Firestore y FCM), estructura de paquetes y un login funcional con enrutado por rol.
**No se pudo compilar en el entorno donde se preparó**: sincroniza y compila en Android Studio y
revisa la sección «Si algo falla».

## 1. Antes de sincronizar Gradle
1. Copia tu `google-services.json` en `app/google-services.json` (su `package_name` debe ser `com.jyhs.educheck`).
   Está en `.gitignore`; cada desarrollador lo descarga de la consola de Firebase.
2. En la consola de Firebase:
   - **Authentication → Sign-in method:** habilita *Correo electrónico/contraseña*.
   - **Firestore Database → Crear base de datos** (modo producción). La ubicación no se puede cambiar después.
   - Publica `firestore.rules` e índices: `firebase deploy --only firestore` (o pega las reglas en la pestaña *Reglas*).
     Modelo y reglas explicados en `docs/MODELO_FIRESTORE.md`.
3. Crea el primer usuario:
   - **Authentication → Users → Agregar usuario** (correo y contraseña) y copia su **UID**.
   - **Firestore → Iniciar colección** `usuarios` → documento con **ID = UID** y los campos
     `rol` (string: `ALUMNO`, `TUTOR`, `PROFESOR`, `PREFECTO` o `ADMIN`) y, opcional, `plantelId` (string).

## 2. Ejecutar
Sincroniza Gradle y ejecuta la app. Con el usuario anterior debes ver el login y, al entrar,
la pantalla provisional «Sesión iniciada como <rol>». Cerrar sesión regresa al login.
Las pruebas quedan para el final del proyecto, cuando se indique.

## 3. Firebase Emulator Suite (opcional, sin gastar cuota)
```
npm install -g firebase-tools
firebase emulators:start        # Auth 9099, Firestore 8080, UI http://localhost:4000
```
En `app/build.gradle.kts`, cambia `USE_FIREBASE_EMULATOR` a `"true"` (tipo `debug`).
La app usa `10.0.2.2` (emulador de Android). Con un teléfono físico, usa la IP de tu computadora en
`FirebaseModule` y en `network_security_config_debug.xml`. Crea el usuario y el documento
`usuarios/{uid}` desde la UI del emulador.

## 3b. Datos de ejemplo para el emulador
```
firebase emulators:start --project <ID_DE_TU_PROYECTO>
cd firebase/seed && npm install && node seed.mjs <ID_DE_TU_PROYECTO>
```
Crea un plantel, un grupo con horario y cinco cuentas (`admin@`, `prefecto@`, `profesor@`, `tutor@`, `alumno@educheck.test`,
contraseña `Educheck123!`). El script solo habla con los emuladores. Si cambias de proyecto, repite con su ID.

## 4. Qué se decidió
- `minSdk = 31` (el documento decía 26): permite leer el BSSID con la API moderna sin rutas de compatibilidad.
- AGP 9 trae Kotlin integrado: no hay plugin `kotlin-android` ni `kapt`; Hilt y Room usan **KSP**.
- El rol se lee de `usuarios/{uid}` en Firestore. Más adelante el backend podrá fijarlo también como *custom claim*.
- La sesión (uid y rol, sin contraseñas ni tokens) se guarda en DataStore para abrir la app sin conexión.
- Room incluye solo la entidad de presencia (`asistencia_local`) del documento; el resto llega con cada módulo.
- Aún no se declaran permisos de ubicación ni cámara: se agregan cuando se construya cada función.
- Navigation Compose queda como dependencia; se usará al crear los grafos por rol.

## 5. Estructura de paquetes
```
com.jyhs.educheck
├── EduCheckApp.kt / MainActivity.kt
├── core/    common · designsystem (Material 3) · navigation
├── domain/  model · repository · usecase
├── data/    local (db, dao, entity, prefs) · firebase · push
├── di/      FirebaseModule · DatabaseModule · RepositoryModule
├── feature_login/  feature_home/ (provisional)
```

## 6. Si algo falla
- **«File google-services.json is missing»:** falta el archivo en `app/`.
- **Errores de Hilt/KSP con JDK 25:** el proyecto usa JDK 25 como JVM del daemon. Prueba con 21:
  `./gradlew updateDaemonJvm --jvm-version=21` y sincroniza de nuevo.
- **Versiones:** si Android Studio sugiere una versión distinta, cámbiala solo en `gradle/libs.versions.toml`.
  Requisitos con AGP 9: KSP ≥ 2.3.6 y Hilt ≥ 2.59.2.
- **Memoria:** `gradle.properties` sube el heap de Gradle a 3 GB por Hilt/KSP/Compose.
