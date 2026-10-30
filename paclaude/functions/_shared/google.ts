import { importPKCS8, SignJWT } from "npm:jose@5";
import { env } from "./env.ts";

const ALCANCES = [
  "https://www.googleapis.com/auth/cloud-platform",
  "https://www.googleapis.com/auth/firebase.messaging",
].join(" ");

let cache: { token: string; expiraEnMs: number } | null = null;

/**
 * Token OAuth2 de la cuenta de servicio de Firebase (flujo JWT bearer), con caché en memoria.
 * Se firma con jose/Web Crypto: el Admin SDK y google-auth-library no funcionan bien en Deno.
 */
export async function tokenDeGoogle(): Promise<string> {
  if (cache && cache.expiraEnMs > Date.now() + 60_000) return cache.token;

  const cuenta = JSON.parse(env("GOOGLE_SERVICE_ACCOUNT_JSON"));
  const llave = await importPKCS8(cuenta.private_key, "RS256");
  const assertion = await new SignJWT({ scope: ALCANCES })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(cuenta.client_email)
    .setSubject(cuenta.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(llave);

  const respuesta = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!respuesta.ok) throw new Error("No se pudo obtener el token de Google: " + respuesta.status);
  const datos = await respuesta.json();
  cache = { token: datos.access_token, expiraEnMs: Date.now() + Number(datos.expires_in) * 1000 };
  return cache.token;
}

/** fetch con el token de la cuenta de servicio. */
export async function googleFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const token = await tokenDeGoogle();
  const cabeceras = new Headers(init.headers);
  cabeceras.set("Authorization", "Bearer " + token);
  if (init.body && !cabeceras.has("Content-Type")) cabeceras.set("Content-Type", "application/json");
  return await fetch(url, { ...init, headers: cabeceras });
}
