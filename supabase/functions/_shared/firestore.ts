import { env } from "./env.ts";
import { googleFetch } from "./google.ts";

// API REST de Firestore con la cuenta de servicio (ignora las reglas de seguridad).
const raiz = () => "projects/" + env("FIREBASE_PROJECT_ID") + "/databases/(default)/documents";
const base = () => "https://firestore.googleapis.com/v1/" + raiz();

export interface Doc {
  id: string;
  data: Record<string, unknown>;
}

// ------------------------------------------------------------------ conversión de valores
// deno-lint-ignore no-explicit-any
export function decodificar(v: any): unknown {
  if (v === undefined || v === null) return null;
  if ("nullValue" in v) return null;
  if ("stringValue" in v) return v.stringValue;
  if ("booleanValue" in v) return v.booleanValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("doubleValue" in v) return v.doubleValue;
  if ("timestampValue" in v) return new Date(v.timestampValue);
  if ("geoPointValue" in v) return { latitud: v.geoPointValue.latitude, longitud: v.geoPointValue.longitude };
  if ("referenceValue" in v) return v.referenceValue;
  if ("arrayValue" in v) return (v.arrayValue.values ?? []).map(decodificar);
  if ("mapValue" in v) return decodificarCampos(v.mapValue.fields ?? {});
  return null;
}

// deno-lint-ignore no-explicit-any
export function decodificarCampos(campos: Record<string, any>): Record<string, unknown> {
  const salida: Record<string, unknown> = {};
  for (const [clave, valor] of Object.entries(campos)) salida[clave] = decodificar(valor);
  return salida;
}

// deno-lint-ignore no-explicit-any
export function codificar(v: unknown): any {
  if (v === null || v === undefined) return { nullValue: null };
  if (typeof v === "string") return { stringValue: v };
  if (typeof v === "boolean") return { booleanValue: v };
  if (typeof v === "number") return Number.isInteger(v) ? { integerValue: String(v) } : { doubleValue: v };
  if (v instanceof Date) return { timestampValue: v.toISOString() };
  if (Array.isArray(v)) return { arrayValue: { values: v.map(codificar) } };
  if (typeof v === "object") return { mapValue: { fields: codificarCampos(v as Record<string, unknown>) } };
  throw new Error("Tipo no soportado para Firestore");
}

// deno-lint-ignore no-explicit-any
export function codificarCampos(obj: Record<string, unknown>): Record<string, any> {
  // deno-lint-ignore no-explicit-any
  const salida: Record<string, any> = {};
  for (const [clave, valor] of Object.entries(obj)) salida[clave] = codificar(valor);
  return salida;
}

const idDe = (nombre: string): string => nombre.substring(nombre.lastIndexOf("/") + 1);

// ------------------------------------------------------------------ lecturas
export async function leerDoc(ruta: string): Promise<Doc | null> {
  const r = await googleFetch(base() + "/" + ruta);
  if (r.status === 404) return null;
  if (!r.ok) throw new Error("Firestore GET " + ruta + ": " + r.status + " " + (await r.text()));
  const doc = await r.json();
  return { id: idDe(doc.name), data: decodificarCampos(doc.fields ?? {}) };
}

/** Lee varios documentos por ruta. El resultado conserva null para los que no existen. */
export async function leerVarios(rutas: string[]): Promise<Map<string, Doc | null>> {
  const resultado = new Map<string, Doc | null>();
  for (let i = 0; i < rutas.length; i += 100) {
    const lote = rutas.slice(i, i + 100);
    const r = await googleFetch(base() + ":batchGet", {
      method: "POST",
      body: JSON.stringify({ documents: lote.map((ruta) => raiz() + "/" + ruta) }),
    });
    if (!r.ok) throw new Error("Firestore batchGet: " + r.status + " " + (await r.text()));
    const items = await r.json();
    for (const item of items) {
      if (item.found) {
        const prefijo = raiz() + "/";
        const ruta = String(item.found.name).substring(prefijo.length);
        resultado.set(ruta, { id: idDe(item.found.name), data: decodificarCampos(item.found.fields ?? {}) });
      } else if (item.missing) {
        const prefijo = raiz() + "/";
        resultado.set(String(item.missing).substring(prefijo.length), null);
      }
    }
  }
  return resultado;
}

// deno-lint-ignore no-explicit-any
export const filtroIgual = (campo: string, valor: unknown): any => ({
  fieldFilter: { field: { fieldPath: campo }, op: "EQUAL", value: codificar(valor) },
});

/** Consulta simple sobre una colección de primer nivel con filtros de igualdad (AND). */
export async function consultar(
  coleccion: string,
  filtros: Array<[string, unknown]>,
  limite = 500,
): Promise<Doc[]> {
  const where = filtros.length === 1
    ? filtroIgual(filtros[0][0], filtros[0][1])
    : { compositeFilter: { op: "AND", filters: filtros.map(([c, v]) => filtroIgual(c, v)) } };
  const r = await googleFetch(base() + ":runQuery", {
    method: "POST",
    body: JSON.stringify({ structuredQuery: { from: [{ collectionId: coleccion }], where, limit: limite } }),
  });
  if (!r.ok) throw new Error("Firestore runQuery " + coleccion + ": " + r.status + " " + (await r.text()));
  const filas = await r.json();
  const docs: Doc[] = [];
  for (const fila of filas) {
    if (fila.document) docs.push({ id: idDe(fila.document.name), data: decodificarCampos(fila.document.fields ?? {}) });
  }
  return docs;
}

// ------------------------------------------------------------------ escrituras
// deno-lint-ignore no-explicit-any
async function commit(escrituras: any[]): Promise<void> {
  const r = await googleFetch(base() + ":commit", {
    method: "POST",
    body: JSON.stringify({ writes: escrituras }),
  });
  if (!r.ok) throw new Error("Firestore commit: " + r.status + " " + (await r.text()));
}

/** Crea o reemplaza un documento completo. */
export async function guardarDoc(ruta: string, data: Record<string, unknown>): Promise<void> {
  await commit([{ update: { name: raiz() + "/" + ruta, fields: codificarCampos(data) } }]);
}

/** Actualiza solo los campos indicados; falla si el documento no existe. */
export async function actualizarCampos(ruta: string, data: Record<string, unknown>): Promise<void> {
  await commit([{
    update: { name: raiz() + "/" + ruta, fields: codificarCampos(data) },
    updateMask: { fieldPaths: Object.keys(data) },
    currentDocument: { exists: true },
  }]);
}

export async function borrarDoc(ruta: string): Promise<void> {
  await commit([{ delete: raiz() + "/" + ruta }]);
}
