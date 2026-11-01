// POST { accion: "crear", email, password, nombre, rol, plantelId?, alumnoId? }
// POST { accion: "actualizar", uid, rol?, activo?, nombre? }          · solo ADMIN
// Crea cuentas de Firebase Auth (Identity Toolkit con la cuenta de servicio), fija el rol como custom
// claim y escribe el perfil en Firestore (usuarios/{uid}). Sustituye al Admin SDK, que no corre en Deno.
import { exigirTexto, HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol, type Rol } from "../_shared/roles.ts";
import { actualizarCampos, borrarDoc, guardarDoc, leerDoc } from "../_shared/firestore.ts";
import { googleFetch } from "../_shared/google.ts";
import { env } from "../_shared/env.ts";

const ROLES: Rol[] = ["ALUMNO", "TUTOR", "PROFESOR", "ADMIN"];
const identidad = () => `https://identitytoolkit.googleapis.com/v1/projects/${env("FIREBASE_PROJECT_ID")}`;

async function llamarIdentidad(ruta: string, cuerpo: Record<string, unknown>): Promise<Record<string, unknown>> {
  const r = await googleFetch(identidad() + ruta, { method: "POST", body: JSON.stringify(cuerpo) });
  const datos = await r.json().catch(() => ({}));
  if (!r.ok) {
    const texto = JSON.stringify(datos);
    if (texto.includes("EMAIL_EXISTS")) throw new HttpError(409, "Ya existe una cuenta con ese correo", "EMAIL_EXISTE");
    if (texto.includes("WEAK_PASSWORD")) throw new HttpError(400, "La contraseña es muy débil", "CLAVE_DEBIL");
    throw new Error("Identity Toolkit " + ruta + ": " + r.status);   // sin volcar el cuerpo (puede traer datos)
  }
  return datos;
}

const claims = (rol: Rol, plantelId: string) => JSON.stringify({ rol, plantelId });

Deno.serve(manejar(async (req) => {
  const admin = await exigirRol(req, "ADMIN");
  const cuerpo = await leerJson<Record<string, unknown>>(req);

  // ------------------------------------------------------------------ crear
  if (cuerpo.accion === "crear") {
    const email = exigirTexto(cuerpo.email, "email", 254).toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new HttpError(400, "Correo inválido");
    const password = exigirTexto(cuerpo.password, "password", 100);
    if (password.length < 8) throw new HttpError(400, "La contraseña debe tener al menos 8 caracteres");
    const nombre = exigirTexto(cuerpo.nombre, "nombre", 150);
    const rol = cuerpo.rol as Rol;
    if (!ROLES.includes(rol)) throw new HttpError(400, "rol inválido");
    const plantelId = typeof cuerpo.plantelId === "string" && cuerpo.plantelId !== "" ? cuerpo.plantelId : admin.plantelId;

    let alumnoId: string | undefined;
    if (rol === "ALUMNO") {
      alumnoId = exigirTexto(cuerpo.alumnoId, "alumnoId", 200);
      const alumno = await leerDoc("alumnos/" + alumnoId);
      if (!alumno) throw new HttpError(404, "El alumno no existe", "ALUMNO_NO_EXISTE");
      if (alumno.data.alumnoUid) throw new HttpError(409, "El alumno ya tiene una cuenta", "ALUMNO_YA_VINCULADO");
    }

    const creada = await llamarIdentidad("/accounts", { email, password, displayName: nombre });
    const uid = String(creada.localId);
    try {
      await llamarIdentidad("/accounts:update", { localId: uid, customAttributes: claims(rol, plantelId) });
      await guardarDoc("usuarios/" + uid, {
        nombre,
        email,
        rol,
        plantelId,
        activo: true,
        creadoEn: new Date(),
        ...(alumnoId ? { alumnoId } : {}),
      });
      if (alumnoId) await actualizarCampos("alumnos/" + alumnoId, { alumnoUid: uid });
    } catch (e) {
      // Deshacer lo creado para no dejar cuentas a medias
      await llamarIdentidad("/accounts:delete", { localId: uid }).catch(() => {});
      await borrarDoc("usuarios/" + uid).catch(() => {});
      throw e;
    }
    return json({ ok: true, uid }, 201);
  }

  // ------------------------------------------------------------------ actualizar
  if (cuerpo.accion === "actualizar") {
    const uid = exigirTexto(cuerpo.uid, "uid", 128);
    const actual = await leerDoc("usuarios/" + uid);
    if (!actual) throw new HttpError(404, "El usuario no existe", "USUARIO_NO_EXISTE");

    const cambios: Record<string, unknown> = {};
    if (cuerpo.nombre !== undefined) cambios.nombre = exigirTexto(cuerpo.nombre, "nombre", 150);
    if (cuerpo.rol !== undefined) {
      if (!ROLES.includes(cuerpo.rol as Rol)) throw new HttpError(400, "rol inválido");
      cambios.rol = cuerpo.rol;
    }
    if (cuerpo.activo !== undefined) {
      if (typeof cuerpo.activo !== "boolean") throw new HttpError(400, "activo debe ser booleano");
      if (uid === admin.uid && cuerpo.activo === false) throw new HttpError(400, "No puedes desactivar tu propia cuenta");
      cambios.activo = cuerpo.activo;
    }
    if (Object.keys(cambios).length === 0) throw new HttpError(400, "No hay cambios que aplicar");

    const cambiosAuth: Record<string, unknown> = { localId: uid };
    if (cambios.rol) cambiosAuth.customAttributes = claims(cambios.rol as Rol, String(actual.data.plantelId ?? ""));
    if (cambios.activo !== undefined) cambiosAuth.disableUser = !cambios.activo;
    if (cambios.nombre) cambiosAuth.displayName = cambios.nombre;
    if (Object.keys(cambiosAuth).length > 1) await llamarIdentidad("/accounts:update", cambiosAuth);

    await actualizarCampos("usuarios/" + uid, cambios);
    return json({ ok: true });
  }

  throw new HttpError(400, "accion debe ser crear o actualizar");
}));
