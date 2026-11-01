/** Lee una variable de entorno obligatoria (o usa el valor por defecto). */
export function env(nombre: string, porDefecto?: string): string {
  const valor = Deno.env.get(nombre) ?? porDefecto;
  if (valor === undefined || valor === "") {
    throw new Error("Falta la variable de entorno " + nombre);
  }
  return valor;
}

export const zonaPlantel = (): string => env("TZ_PLANTEL", "America/Mexico_City");

/** El aviso «sin contacto» se activa cuando la app ya envía latidos (etapa posterior). */
export const avisoSinContactoActivo = (): boolean => env("AVISO_SIN_CONTACTO", "false") === "true";

/** Días que se conservan las evidencias antes de borrarlas. */
export const retencionDias = (): number => Number(env("RETENCION_DIAS", "30"));
