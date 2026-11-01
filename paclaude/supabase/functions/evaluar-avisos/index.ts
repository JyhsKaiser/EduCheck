// POST · llamada por pg_cron cada 5 minutos en horario escolar (cabecera x-cron-secret)
//  A) Ausencia: cuando vence «inicio de la 1.ª clase + tolerancia» de un grupo, los alumnos sin presencia
//     reciben un documento ausencias/{alumno}_{fecha} y sus tutores un push de prioridad alta.
//  B) Sin contacto: alumnos presentes que dejaron de mandar latidos (solo si AVISO_SIN_CONTACTO=true).
import { json, manejar } from "../_shared/http.ts";
import { exigirCron } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";
import { guardarDoc, leerDoc, leerVarios } from "../_shared/firestore.ts";
import { enviarAUsuarios, marcarEnviado } from "../_shared/avisos.ts";
import { fechaLocal, hhmmAMinutos, minutosAHora, minutosLocales } from "../_shared/tiempo.ts";
import { avisoSinContactoActivo, zonaPlantel } from "../_shared/env.ts";

interface GrupoDia {
  grupo_id: string;
  plantel_id: string;
  inicio: string;
  tolerancia_min: number;
}

async function procesarAusencias(grupo: GrupoDia, fecha: string, ahora: Date): Promise<number> {
  const doc = await leerDoc("grupos/" + grupo.grupo_id);
  const lista = doc && Array.isArray(doc.data.alumnos) ? (doc.data.alumnos as Array<Record<string, unknown>>) : [];
  if (lista.length === 0) return 0;

  const presencias = await leerVarios(lista.map((a) => `presencias/${a.id}_${fecha}`));
  let total = 0;
  for (const alumno of lista) {
    const alumnoId = String(alumno.id);
    if (presencias.get(`presencias/${alumnoId}_${fecha}`)) continue;     // ya llegó

    if (!(await marcarEnviado(`AUSENCIA:${alumnoId}:${fecha}`, "AUSENCIA"))) continue;
    const tutores = Array.isArray(alumno.tutorUids) ? (alumno.tutorUids as string[]) : [];
    await guardarDoc(`ausencias/${alumnoId}_${fecha}`, {
      alumnoId,
      plantelId: grupo.plantel_id,
      grupoId: grupo.grupo_id,
      fecha,
      estado: "AUSENTE",
      tutorUids: tutores,
      creadoEn: ahora,
    });
    await enviarAUsuarios(tutores, {
      tipo: "INASISTENCIA",
      titulo: "Sin registro de entrada",
      cuerpo: `${alumno.nombre ?? "Tu hijo(a)"} no ha registrado su entrada al plantel.`,
      canal: "inasistencias",
      alta: true,
      datos: { alumnoId, fecha },
    });
    total++;
  }
  return total;
}

Deno.serve(manejar(async (req) => {
  exigirCron(req);
  const db = getDb();
  const zona = zonaPlantel();
  const ahora = new Date();
  const fecha = fechaLocal(ahora, zona);
  const minutosAhora = minutosLocales(ahora, zona);
  const resumen = { gruposEvaluados: 0, ausencias: 0, sinContacto: 0 };

  // ------------------------------------------------------------------ A) ausencias
  const { data: pendientes, error } = await db.from("horarios_dia").select("grupo_id,plantel_id,inicio,tolerancia_min")
    .eq("fecha", fecha).is("ausencia_evaluada_en", null);
  if (error) throw new Error("horarios_dia: " + error.message);

  for (const grupo of (pendientes ?? []) as GrupoDia[]) {
    if (minutosAhora < hhmmAMinutos(grupo.inicio) + grupo.tolerancia_min) continue;   // aún no vence

    // Se «reclama» el grupo antes de procesarlo: si dos ejecuciones se solapan, solo una lo evalúa
    const { data: reclamado } = await db.from("horarios_dia")
      .update({ ausencia_evaluada_en: ahora.toISOString() })
      .eq("fecha", fecha).eq("grupo_id", grupo.grupo_id).is("ausencia_evaluada_en", null)
      .select("grupo_id");
    if (!reclamado || reclamado.length === 0) continue;

    try {
      resumen.ausencias += await procesarAusencias(grupo, fecha, ahora);
      resumen.gruposEvaluados++;
    } catch (e) {
      console.error("Error evaluando el grupo " + grupo.grupo_id, e);
      // Se libera para que el siguiente ciclo lo reintente (los avisos ya enviados no se repiten)
      await db.from("horarios_dia").update({ ausencia_evaluada_en: null })
        .eq("fecha", fecha).eq("grupo_id", grupo.grupo_id);
    }
  }

  // ------------------------------------------------------------------ B) sin contacto
  if (avisoSinContactoActivo()) {
    const { data: candidatos, error: errorRpc } = await db.rpc("candidatos_sin_contacto", {
      p_fecha: fecha,
      p_ahora: ahora.toISOString(),
      p_hora_local: minutosAHora(minutosAhora),
    });
    if (errorRpc) throw new Error("candidatos_sin_contacto: " + errorRpc.message);

    for (const c of (candidatos ?? []) as Array<{ alumno_id: string; nombre: string | null; tutor_uids: string[] }>) {
      if (!(await marcarEnviado(`SINCONTACTO:${c.alumno_id}:${fecha}`, "SIN_CONTACTO"))) continue;
      await enviarAUsuarios(c.tutor_uids, {
        tipo: "SIN_CONTACTO",
        titulo: "Sin contacto con el dispositivo",
        cuerpo: `No hemos recibido señal del dispositivo de ${c.nombre ?? "tu hijo(a)"} en la última hora.`,
        canal: "inasistencias",
        alta: true,
        datos: { alumnoId: c.alumno_id, fecha },
      });
      resumen.sinContacto++;
    }
  }
  return json({ ok: true, fecha, ...resumen });
}));
