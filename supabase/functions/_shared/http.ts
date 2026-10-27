export class HttpError extends Error {
  constructor(public status: number, message: string, public codigo?: string) {
    super(message);
  }
}

const cabecerasCors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, content-type, x-cron-secret",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
};

export function json(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...cabecerasCors },
  });
}

/** Envuelve una función: CORS, errores controlados y errores inesperados sin filtrar detalles. */
export function manejar(fn: (req: Request) => Promise<Response>): (req: Request) => Promise<Response> {
  return async (req: Request) => {
    if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cabecerasCors });
    try {
      return await fn(req);
    } catch (e) {
      if (e instanceof HttpError) return json({ error: e.message, codigo: e.codigo ?? null }, e.status);
      console.error("Error no controlado:", e);
      return json({ error: "Error interno" }, 500);
    }
  };
}

export async function leerJson<T>(req: Request): Promise<T> {
  if (req.method !== "POST") throw new HttpError(405, "Usa POST");
  try {
    return (await req.json()) as T;
  } catch {
    throw new HttpError(400, "El cuerpo debe ser JSON válido");
  }
}

export function exigirTexto(valor: unknown, nombre: string, max = 200): string {
  if (typeof valor !== "string" || valor.trim() === "" || valor.length > max) {
    throw new HttpError(400, "Campo inválido: " + nombre);
  }
  return valor.trim();
}

/** Comparación en tiempo constante de dos textos. */
export function igualesSeguro(a: string, b: string): boolean {
  const ea = new TextEncoder().encode(a);
  const eb = new TextEncoder().encode(b);
  if (ea.length !== eb.length) return false;
  let diff = 0;
  for (let i = 0; i < ea.length; i++) diff |= ea[i] ^ eb[i];
  return diff === 0;
}
