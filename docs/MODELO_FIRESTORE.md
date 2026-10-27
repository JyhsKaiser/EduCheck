# EduCheck · Modelo de datos de Firestore y reglas (etapa 1)

Fuente de verdad: Firestore (plan Spark). Este modelo se diseñó para **cuota gratuita** (50 000 lecturas y
20 000 escrituras al día) y para funcionar **sin backend** en esta etapa. Lo que sí necesita backend
(Admin SDK en Azure) está marcado en la sección 6.

## 1. Principios de diseño
1. **Identificadores deterministas** (`{alumnoId}_{fecha}`, `{horarioId}_{fecha}`): garantizan una sola presencia por alumno y día y una sola sesión por horario y día, sin transacciones.
2. **Una escritura por acción**: el pase de lista guarda *toda* la lista en un solo documento (mapa), no un documento por alumno.
3. **Desnormalizar para las reglas**: `tutorUids` se copia a cada presencia, y `profesorUids` + lista de alumnos viven en `grupos`. Las reglas de consulta no pueden “seguir” relaciones, así que cada lector necesita un campo contra el cual validar.
4. **Las reglas validan lo que importa**: dentro de la geocerca (`latlng.distance`), dispositivo `ACTIVO`, hora razonable, estructura y propiedad. Los clientes no escriben `AUSENTE`, avisos ni calificaciones.
5. **Contadores sin escrituras extra**: el dashboard usa consultas `count()` (se cobra 1 lectura por cada 1 000 documentos).

## 2. Mapa de colecciones
| Colección | docId | Contenido principal | Escribe | Lee |
|---|---|---|---|---|
| `usuarios` | uid de Auth | `nombre`, `email`, `rol`, `plantelId`, `activo`, `alumnoId?` | ADMIN | el usuario; ADMIN |
| `planteles` | id | `nombre`, `centro` (GeoPoint), `radioMetros`, `toleranciaMetros` (100), `toleranciaMinutos` (60), `zonaHoraria`, `bssids[]`, `activo` | ADMIN | usuarios del plantel |
| `dispositivos` | `installId` (UUID de la app) | `usuarioUid`, `alumnoId?`, `rol`, `estado` (PENDIENTE/ACTIVO/REVOCADO), `clavePublica`, `modelo`, `fcmToken`, `creadoEn` | la app (alta y token); ADMIN (estado) | dueño; ADMIN |
| `alumnos` | id | `matricula`, `nombres`, `apellidos`, `plantelId`, `grupoId`, `alumnoUid` (nullable), `tutorUids[]`, `activo` | ADMIN | alumno, tutores, staff del plantel |
| `alumnos/{id}/secretos/totp` | `totp` | secreto del QR | ADMIN | el alumno (decisión del QR pendiente) |
| `presencias` | `{alumnoId}_{yyyy-MM-dd}` | `alumnoId`, `alumnoUid`, `plantelId`, `fecha`, `estado`, `metodo`, `confianza`, `ubicacion`, `bssid?`, `horaEntrada`, `horaSalida?`, `relojConfiable`, `registradoPor?`, `tutorUids[]`, `dispositivoId`, `clientUuid`, `creadoEn` | alumno (entrada y salida), prefecto (QR), ADMIN | alumno, tutores, staff |
| `justificantes` | id | `alumnoId`, `plantelId`, `tutorUid`, `tipo`, `fechaInicio`, `fechaFin`, `motivo`, `evidenciaBlobPath?`, `evidenciaTipo?`, `estado`, `comentarioRevision?`, `revisadoPor?`, `revisadoEn?`, `clientUuid`, `creadoEn` | tutor (alta); staff (resolución) | tutor, staff |
| `grupos` | id | `plantelId`, `nombre`, `semestre`, `turno`, `periodo`, `profesorUids[]`, `alumnos[{id,nombre,matricula}]` | ADMIN | profesores del grupo, staff |
| `materias` | id | `clave`, `nombre` | ADMIN | cualquier usuario |
| `asignaciones` | id | `grupoId`, `materiaId`, `materiaNombre`, `grupoNombre`, `profesorUid`, `plantelId` | ADMIN | su profesor, staff |
| `horarios` | id | `asignacionId`, `grupoId`, `plantelId`, `profesorUid`, `materiaNombre`, `grupoNombre`, `diaSemana` (1–7), `horaInicio`, `horaFin`, `aula` | ADMIN | su profesor, staff |
| `sesiones` | `{horarioId}_{yyyy-MM-dd}` | `horarioId`, `asignacionId`, `grupoId`, `plantelId`, `profesorUid`, `fecha`, `materiaNombre`, `grupoNombre`, `horaInicio`, `horaFin`, `estado` (ABIERTA/CERRADA), `lista{alumnoId:{estado,justificada}}` | profesor (mientras ABIERTA); ADMIN | su profesor, staff |
| `calificaciones` | `{asignacionId}_{alumnoId}` | `plantelId`, `asignacionId`, `alumnoId`, `profesorUid`, `parciales{1,2,3}`, `importadoEn` | ADMIN (importación) | su profesor, ADMIN |
| `emergencias` | id | `profesorUid`, `plantelId`, `sesionId?`, `mensaje`, `estado` (ABIERTA/ATENDIDA), `creadoEn`, `atendidaPor?`, `atendidaEn?` | profesor (alta); ADMIN (atender) | autor, ADMIN |
| `notificaciones` | id | `destinatarioUid`, `tipo`, `titulo`, `cuerpo`, `alumnoId?`, `creadoEn`, `leida` | **solo backend** | destinatario (puede marcar `leida`) |

`ADMIN` cumple también la función de **coordinador académico**. `staff` = ADMIN o PREFECTO del plantel.

## 3. Reglas relevantes (`firestore.rules`)
- **Rol:** se lee de `usuarios/{uid}` con `get()`. Cada evaluación cuesta una lectura; más adelante puede pasarse a *custom claims*.
- **Presencia del alumno (create):** campos permitidos y requeridos; `alumnoUid == request.auth.uid`; `plantelId` y `tutorUids` iguales a los del alumno; `estado == 'PRESENTE'`; `metodo` ∈ {`GEOCERCA`, `GEOCERCA_WIFI`}; `confianza` coherente con el método; `ubicacion.distance(plantel.centro) <= radioMetros + toleranciaMetros`; con Wi-Fi, el `bssid` debe estar en `plantel.bssids`; `horaEntrada` no futura ni mayor a 24 h (para soportar la cola offline); `creadoEn == request.time`; y el dispositivo debe existir, estar `ACTIVO` y ser del alumno. Usa 3 llamadas `get()` (límite: 10).
- **Presencia por QR (create):** solo prefecto del plantel o ADMIN, `metodo == 'QR_PREFECTURA'`, `registradoPor == uid`.
- **Salida (update):** solo el alumno, solo `horaSalida`, nunca hacia atrás.
- **Pase de lista:** el profesor crea `sesiones/{horarioId}_{fecha}` solo si el horario es suyo; puede corregir mientras esté `ABIERTA`; después, solo ADMIN.
- **Justificantes:** el tutor del alumno crea con `estado == 'PENDIENTE'` y una ruta de evidencia que empieza con su uid; staff resuelve (`ACEPTADO`/`RECHAZADO`) y deja su uid y la hora del servidor.
- **Dispositivos:** el alumno solicita con `estado == 'PENDIENTE'`; los demás roles quedan `ACTIVO`. El dueño solo cambia `fcmToken`; el ADMIN aprueba o revoca.
- **Todo lo no listado** queda cerrado.

## 4. Consultas por pantalla e índices (`firestore.indexes.json`)
| Pantalla | Consulta | Índice |
|---|---|---|
| Tutor · tablero | `alumnos` con `tutorUids array-contains uid`; `presencias` con `tutorUids array-contains uid` y `fecha == hoy` | compuesto `presencias(tutorUids, fecha)` |
| Tutor · historial | igual, con `fecha >= desde` | mismo índice |
| Prefecto/Admin · dashboard | `count()` de `presencias` con `plantelId`, `fecha`, `estado` (igualdades) | no requiere |
| Prefecto/Admin · en vivo | `presencias` con `plantelId`, `fecha == hoy`, ordenado por `horaEntrada` desc | `presencias(plantelId, fecha, horaEntrada)` |
| Profesor · mi día | `horarios` con `profesorUid` y `diaSemana == hoy`, ordenado por `horaInicio`; `sesiones` con `profesorUid` y `fecha == hoy` | `horarios(profesorUid, diaSemana, horaInicio)` y `sesiones(profesorUid, fecha, horaInicio)` |
| Profesor · lista | lectura de `grupos/{grupoId}` (lista de alumnos) | no requiere |
| Admin · justificantes | `plantelId`, `estado == PENDIENTE`, orden por `creadoEn` desc | `justificantes(plantelId, estado, creadoEn)` |
| Tutor · mis justificantes | `tutorUid`, orden por `creadoEn` desc | `justificantes(tutorUid, creadoEn)` |
| Admin · emergencias | `plantelId`, `estado == ABIERTA`, orden por `creadoEn` desc | `emergencias(plantelId, estado, creadoEn)` |

## 5. Presupuesto de cuota (estimación propia, orden de magnitud)
Supuestos: 500 alumnos, 400 tutores, 15 grupos con 6 clases al día, 20 profesores.
| Concepto | Escrituras/día | Lecturas/día |
|---|---|---|
| Presencias (entrada y salida) | ≈ 1 000 | ≈ 1 500 (3 `get()` de reglas por entrada) |
| Pase de lista (90 sesiones + correcciones) | ≈ 120 | ≈ 100 |
| Justificantes, emergencias, dispositivos | ≈ 30 | ≈ 100 |
| Tablero de tutores (4 aperturas al día) | — | ≈ 4 800 |
| Actualizaciones en tiempo real a tutores | — | ≈ 1 300 |
| Profesores (horarios, grupo, sesiones) | — | ≈ 600 |
| Dashboard y escucha de prefectura | — | ≈ 1 500 |
| **Total aproximado** | **≈ 1 150 (≈ 6 % de 20 000)** | **≈ 10 000 (≈ 20 % de 50 000)** |

Cuidados: una importación de calificaciones de 3 000 documentos consume el 15 % de las escrituras del día; un *listener* que se reconecta tras más de 30 minutos sin conexión vuelve a leer su resultado; y los latidos **no** deben escribirse en Firestore (36 escrituras por alumno al día ≈ 18 000 con 500 alumnos).

## 6. Lo que necesita backend (Admin SDK en Azure)
| Requerimiento | Por qué no se puede solo con Firestore |
|---|---|
| RF-T02, RF-T03, RF-B04, RF-B05, RF-PF04 (push) | Spark no incluye Cloud Functions: algo debe enviar FCM al cambiar un documento. |
| Escribir `AUSENTE` y evaluar tolerancias (RF-B01) | Necesita un temporizador. Mientras tanto, la app puede **calcular** «sin registro» localmente. |
| Latidos y pérdida de contacto (RF-B05) | Escribirlos en Firestore agota la cuota; el backend los guarda en memoria. |
| Enlaces SAS de Azure Blob (RF-T04) | Requiere la clave de la cuenta de almacenamiento. |
| Verificación del TOTP del QR (RF-A03, P01) | Las reglas no calculan HMAC. |
| Custom claims de rol (opcional) | Solo con Admin SDK. |

## 7. Decisiones abiertas
1. **Rol por documento o por claims.** Se empieza por documento (sin backend); migrar después es sencillo.
2. **Ubicación validada en reglas.** Evita escrituras fuera del radio, pero no detecta una ubicación simulada; se complementa con `isMock`, el QR y el pase de lista.
3. **Lista de alumnos del grupo** (`grupos.alumnos`) la mantiene el ADMIN junto con `alumnos.grupoId`; hay que actualizarlos en un solo *batch*.
4. **Fecha de la sesión** no se valida contra el reloj del servidor (solo la hora de creación).
5. **Quién verifica el TOTP:** escáner (necesitaría leer secretos) o backend.
6. **Visibilidad para tutores y alumnos** de la asistencia por clase y de las calificaciones: hoy no tienen acceso.
7. **Altas de usuarios desde la app del ADMIN** sin backend: crear la cuenta con una instancia secundaria de `FirebaseApp` y escribir el perfil en `usuarios`.
