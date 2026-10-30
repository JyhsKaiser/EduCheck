Deno.serve(() =>
  new Response(JSON.stringify({ ok: true, service: "educheck" }), {
    headers: { "Content-Type": "application/json" },
  })
);