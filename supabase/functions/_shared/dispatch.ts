// RF-T02: tells the guardians when their student's entry is registered.
//
// Run every minute by pg_cron. It looks for presences created since the last run (a cursor kept in Postgres,
// with a small overlap because commits are not perfectly ordered). For every guardian it first creates
// notifications/{presenceId}_{guardianUid} with the precondition "does not exist": if it already exists the
// guardian was told in an earlier run and nothing is sent. Only then the push is sent (at most once).
import { notificationWrite, pushToUsers } from "./fcm.ts";
import { fsCommit, fsGetFields, fsRunQuery, fsStringArray, fsString, fsTimestamp } from "./firestore.ts";
import { HttpError } from "./http.ts";
import { getCursor, setCursor } from "./state.ts";

const CURSOR_NAME = "presence-entry";
const OVERLAP_MS = 2 * 60_000;
const FIRST_RUN_LOOKBACK_MS = 10 * 60_000;
const PAGE_SIZE = 200;
const MAX_PAGES = 5;

export type DispatchSummary = { presences: number; notified: number; skipped: number; pushes: number };

function formatTime(date: Date, timeZone: string): string {
  return new Intl.DateTimeFormat("es-MX", { timeZone, hour: "2-digit", minute: "2-digit", hour12: false }).format(date);
}

export async function dispatchEntries(now: Date = new Date()): Promise<DispatchSummary> {
  const summary: DispatchSummary = { presences: 0, notified: 0, skipped: 0, pushes: 0 };
  const cursor = (await getCursor(CURSOR_NAME)) ?? new Date(now.getTime() - FIRST_RUN_LOOKBACK_MS);

  const campus = await fsGetFields("config/campus");
  const timeZone = (campus && fsString(campus, "timeZone")) || "America/Mexico_City";
  const studentNames = new Map<string, string>();

  let from = new Date(cursor.getTime() - OVERLAP_MS);
  let newest = cursor;

  for (let page = 0; page < MAX_PAGES; page++) {
    const docs = await fsRunQuery({
      from: [{ collectionId: "presences" }],
      where: { fieldFilter: { field: { fieldPath: "createdAt" }, op: "GREATER_THAN", value: { timestampValue: from.toISOString() } } },
      orderBy: [{ field: { fieldPath: "createdAt" }, direction: "ASCENDING" }],
      limit: PAGE_SIZE,
    });

    for (const doc of docs) {
      summary.presences++;
      const createdAt = fsTimestamp(doc.fields, "createdAt");
      if (createdAt && createdAt > newest) newest = createdAt;

      const studentId = fsString(doc.fields, "studentId");
      const guardians = fsStringArray(doc.fields, "guardianUids");
      const entryTime = fsTimestamp(doc.fields, "entryTime") ?? createdAt ?? now;
      if (!studentId || guardians.length === 0) continue;

      let name = studentNames.get(studentId);
      if (name === undefined) {
        const student = await fsGetFields(`students/${studentId}`);
        name = student
          ? `${fsString(student, "firstNames") ?? ""} ${fsString(student, "lastNames") ?? ""}`.trim() || "Tu alumno"
          : "Tu alumno";
        studentNames.set(studentId, name);
      }
      const title = "Entrada registrada";
      const body = `${name} llegó al plantel a las ${formatTime(entryTime, timeZone)}.`;

      const fresh: string[] = [];
      for (const uid of guardians) {
        try {
          await fsCommit([notificationWrite(`${doc.id}_${uid}`, { recipientUid: uid, type: "CHECK_IN", title, body, studentId })]);
          fresh.push(uid);
        } catch (e) {
          if (e instanceof HttpError && e.code === "already_exists") summary.skipped++; // told in an earlier run
          else throw e;
        }
      }
      if (fresh.length > 0) {
        summary.notified += fresh.length;
        const result = await pushToUsers(fresh, {
          type: "CHECK_IN",
          title,
          body,
          data: { studentId, presenceId: doc.id },
        });
        summary.pushes += result.sent;
      }
    }

    if (docs.length < PAGE_SIZE) break;
    from = newest;
  }

  // With nothing new, still move forward so the next query stays small.
  const idleCursor = new Date(now.getTime() - OVERLAP_MS);
  await setCursor(CURSOR_NAME, newest > idleCursor ? newest : idleCursor > cursor ? idleCursor : cursor);
  return summary;
}
