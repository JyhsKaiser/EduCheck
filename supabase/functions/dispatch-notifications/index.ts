// dispatch-notifications: called every minute by pg_cron (header x-cron-secret) or by an ADMIN (Bearer token)
// to run it by hand. Deployed with --no-verify-jwt: the caller is authenticated here.
import { verifyCaller } from "../_shared/firebase.ts";
import { dispatchEntries } from "../_shared/dispatch.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") throw new HttpError(405, "method_not_allowed");

    const secret = Deno.env.get("CRON_SECRET");
    const fromCron = !!secret && req.headers.get("x-cron-secret") === secret;
    if (!fromCron) {
      const caller = await verifyCaller(req);
      if (caller.role !== "ADMIN") throw new HttpError(403, "forbidden");
    }

    const summary = await dispatchEntries();
    return json({ ok: true, ...summary });
  } catch (e) {
    return errorResponse(e);
  }
});
