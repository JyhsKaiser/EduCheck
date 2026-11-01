/** Fecha local yyyy-MM-dd en la zona indicada. */
export function fechaLocal(fecha: Date, zona: string): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: zona, year: "numeric", month: "2-digit", day: "2-digit" })
    .format(fecha);
}

/** Minutos transcurridos desde la medianoche local. */
export function minutosLocales(fecha: Date, zona: string): number {
  const partes = new Intl.DateTimeFormat("en-GB", { timeZone: zona, hour: "2-digit", minute: "2-digit", hourCycle: "h23" })
    .formatToParts(fecha);
  const hora = Number(partes.find((p) => p.type === "hour")?.value ?? "0");
  const minuto = Number(partes.find((p) => p.type === "minute")?.value ?? "0");
  return hora * 60 + minuto;
}

/** Día de la semana local: 1 = lunes ... 7 = domingo. */
export function diaSemanaLocal(fecha: Date, zona: string): number {
  const nombre = new Intl.DateTimeFormat("en-US", { timeZone: zona, weekday: "short" }).format(fecha);
  const mapa: Record<string, number> = { Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6, Sun: 7 };
  return mapa[nombre] ?? 1;
}

/** "07:30" o "07:30:00" -> minutos. */
export function hhmmAMinutos(texto: string): number {
  const [h, m] = texto.split(":");
  return Number(h) * 60 + Number(m);
}

/** minutos -> "HH:MM:00" (formato de columna time). */
export function minutosAHora(min: number): string {
  const h = Math.floor(min / 60).toString().padStart(2, "0");
  const m = (min % 60).toString().padStart(2, "0");
  return h + ":" + m + ":00";
}

/** Hora local "HH:mm" para mostrar en los avisos. */
export function horaTexto(fecha: Date, zona: string): string {
  return new Intl.DateTimeFormat("en-GB", { timeZone: zona, hour: "2-digit", minute: "2-digit", hourCycle: "h23" })
    .format(fecha);
}
