// POST · llamada por pg_cron (cabecera x-cron-secret)
// Cada mañana calcula, por grupo, la primera hora de inicio y la última de fin del día
// leyendo los horarios de Firestore (≈ 1 lectura por horario). Con eso el resto del día
// los avisos se evalúan sin volver a leer Firestore.
import { json, manejar } from "../_shared/http.ts";
import { exigirCron } from "../_shared/roles.ts";
import { consultar, leerDoc } from "../_shared/firestore.ts";
import { getDb } from "../_shared/db.ts";
import { diaSemanaLocal, fechaLocal, hhmmAMinutos, minutosAHora } from "../_shared/tiempo.ts";
import { zonaPlantel } from "../_shared/env.ts";

Deno.serve(manejar(async (req) => {
  exigirCron(req);
  const zona = zonaPlantel();
  const ahora = new Date();
  const fecha = fechaLocal(ahora, zona);
  const dia = diaSemanaLocal(ahora, zona);

  const horarios = await consultar("horarios", [["diaSemana", dia]], 2000);
  const porGrupo = new Map<string, { plantelId: string; inicio: number; fin: number }>();
  for (const h of horarios) {
    const grupoId = String(h.data.grupoId);
    const inicio = hhmmAMinutos(String(h.data.horaInicio));
    const fin = hhmmAMinutos(String(h.data.horaFin));
    const previo = porGrupo.get(grupoId);
    if (!previo) {
      porGrupo.set(grupoId, { plantelId: String(h.data.plantelId), inicio, fin });
    } else {
      previo.inicio = Math.min(previo.inicio, inicio);
      previo.fin = Math.max(previo.fin, fin);
    }
  }

  // Tolerancia y zona horaria de cada plantel
  const planteles = new Map<string, { tolerancia: number; zona: string }>();
  for (const plantelId of new Set([...porGrupo.values()].map((g) => g.plantelId))) {
    const doc = await leerDoc("planteles/" + plantelId);
    planteles.set(plantelId, {
      tolerancia: Number(doc?.data.toleranciaMinutos ?? 60),
      zona: String(doc?.data.zonaHoraria ?? zona),
    });
  }

  const filas = [...porGrupo.entries()].map(([grupoId, g]) => ({
    fecha,
    grupo_id: grupoId,
    plantel_id: g.plantelId,
    inicio: minutosAHora(g.inicio),
    fin: minutosAHora(g.fin),
    tolerancia_min: planteles.get(g.plantelId)!.tolerancia,
    zona_horaria: planteles.get(g.plantelId)!.zona,
  }));
  if (filas.length > 0) {
    // No se toca ausencia_evaluada_en: si se vuelve a sincronizar, no se reevalúa el día
    const { error } = await getDb().from("horarios_dia").upsert(filas, { onConflict: "fecha,grupo_id" });
    if (error) throw new Error("horarios_dia: " + error.message);
  }
  return json({ ok: true, fecha, dia, grupos: filas.length });
}));
