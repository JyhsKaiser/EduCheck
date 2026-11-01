import { createRemoteJWKSet, jwtVerify } from "npm:jose@5";
import { env } from "./env.ts";
import { HttpError } from "./http.ts";

// Claves públicas con las que Google firma los ID tokens de Firebase
const JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

export interface UsuarioFirebase {
  uid: string;
  email?: string;
  claims: Record<string, unknown>;
}

/** Valida el ID token de Firebase del encabezado Authorization: Bearer <token>. */
export async function verificarToken(req: Request): Promise<UsuarioFirebase> {
  const cabecera = req.headers.get("Authorization") ?? "";
  if (!cabecera.startsWith("Bearer ")) throw new HttpError(401, "Falta el token de sesión");
  const proyecto = env("FIREBASE_PROJECT_ID");
  try {
    const { payload } = await jwtVerify(cabecera.slice(7), JWKS, {
      issuer: "https://securetoken.google.com/" + proyecto,
      audience: proyecto,
      algorithms: ["RS256"],
    });
    if (!payload.sub) throw new Error("token sin sub");
    return { uid: payload.sub, email: payload.email as string | undefined, claims: payload };
  } catch (_e) {
    throw new HttpError(401, "Token inválido o vencido");
  }
}
