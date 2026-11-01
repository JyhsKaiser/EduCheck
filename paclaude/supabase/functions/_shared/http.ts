// Shared HTTP helpers for the EduCheck Edge Functions.

export class HttpError extends Error {
  constructor(public status: number, public code: string) {
    super(code);
  }
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}

/** Known errors keep their status and code; anything else is logged and returned as a generic 500. */
export function errorResponse(e: unknown): Response {
  if (e instanceof HttpError) return json({ ok: false, error: e.code }, e.status);
  console.error("Unexpected error:", e);
  return json({ ok: false, error: "internal_error" }, 500);
}
