#!/usr/bin/env node
// ===========================================================================
// check-supabase-schema.mjs
//
// Detector de schema drift, somente leitura. Compara o que o código (site +
// Android) espera que exista no banco Supabase contra o que o PostgREST
// realmente enxerga hoje, usando só a anon key (o mesmo acesso que qualquer
// visitante do site já tem - nenhum privilégio elevado é necessário).
//
// NÃO faz nenhuma escrita, NÃO aplica migration, NÃO altera nada. Só lê.
//
// Uso:
//   node scripts/check-supabase-schema.mjs
//
// Credenciais: lidas de (nessa ordem) variáveis de ambiente já exportadas,
// ou de web/.env.local (o mesmo arquivo que o site usa localmente).
//   NEXT_PUBLIC_SUPABASE_URL
//   NEXT_PUBLIC_SUPABASE_ANON_KEY
//
// Saída: termina com exatamente uma das linhas:
//   SCHEMA OK
//   SCHEMA DRIFT DETECTED
// e um exit code: 0 = OK, 1 = drift detectado, 2 = não foi possível checar
// (sem credenciais ou sem rede) - nunca reporta "SCHEMA OK" quando não
// conseguiu de fato verificar.
// ===========================================================================

import { readFileSync, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(__dirname, "..");

function loadEnvLocal() {
  const envPath = join(repoRoot, "web", ".env.local");
  if (!existsSync(envPath)) return {};
  const out = {};
  for (const line of readFileSync(envPath, "utf8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eq = trimmed.indexOf("=");
    if (eq === -1) continue;
    out[trimmed.slice(0, eq).trim()] = trimmed.slice(eq + 1).trim();
  }
  return out;
}

const fileEnv = loadEnvLocal();
const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL || fileEnv.NEXT_PUBLIC_SUPABASE_URL;
const SUPABASE_ANON_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || fileEnv.NEXT_PUBLIC_SUPABASE_ANON_KEY;

if (!SUPABASE_URL || !SUPABASE_ANON_KEY) {
  console.error("Não foi possível checar: NEXT_PUBLIC_SUPABASE_URL / NEXT_PUBLIC_SUPABASE_ANON_KEY");
  console.error("não estão disponíveis (nem no ambiente, nem em web/.env.local).");
  console.error("");
  console.error("Isto NÃO significa que o schema está OK - significa que este script não tem");
  console.error("como verificar a partir deste ambiente de execução.");
  process.exit(2);
}

// Tabelas que o código (site ou Android) referencia hoje. Ver
// supabase/MIGRATION_INVENTORY.md para a origem de cada uma.
const TABLES = [
  "churches",
  "profiles",
  "scales",
  "doxologies",
  "announcements",
  "retrospective_items",
  "worship_songs",
  "web_push_subscriptions",
  "bulletins",
  "plans",
  "subscriptions",
  "export_audit_log",
  "shared_files",
  "organization_roles",
  "organization_teams",
  "organization_people",
  "person_team_memberships",
  "scale_assignments",
  "scale_templates",
];

// Colunas que já causaram incidente real ("Could not find ... in the schema
// cache") por o frontend ter sido atualizado antes do banco - a lista cresce
// conforme novos incidentes acontecem, não é exaustiva de toda coluna que existe.
const CRITICAL_COLUMNS = [
  ["scales", "is_temporary"],
  ["scale_templates", "is_protected"],
  ["worship_songs", "is_daily_recommendation"],
  ["worship_songs", "recommendation_date"],
  ["worship_songs", "recommendation_message"],
  ["worship_songs", "notification_enabled"],
];

// RPCs `security definer`, `stable`, somente leitura (confirmado lendo o SQL
// das migrations 012/013 antes de chamá-las aqui) - seguro sondar com
// parâmetros inofensivos que não batem com nenhuma linha real.
const RPCS = [
  { name: "get_church_subscription", params: { p_church_id: "00000000-0000-0000-0000-000000000000" } },
  { name: "get_church_by_code", params: { p_code: "__schema_probe__" } },
];

const headers = {
  apikey: SUPABASE_ANON_KEY,
  Authorization: `Bearer ${SUPABASE_ANON_KEY}`,
};

async function checkTable(table) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?select=id&limit=1`, { headers });
  if (res.status === 200 || res.status === 206) return { ok: true };
  const body = await res.json().catch(() => ({}));
  if (body.code === "PGRST205") return { ok: false, reason: `Missing table: public.${table}` };
  return { ok: false, reason: `Unexpected error checking table public.${table}: HTTP ${res.status} ${body.message ?? ""}`.trim() };
}

async function checkColumn(table, column) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?select=${column}&limit=1`, { headers });
  if (res.status === 200 || res.status === 206) return { ok: true };
  const body = await res.json().catch(() => ({}));
  if (body.code === "PGRST205") return { ok: true }; // table itself missing - already reported by checkTable
  if (body.code === "PGRST204") return { ok: false, reason: `Missing column: public.${table}.${column}` };
  return { ok: false, reason: `Unexpected error checking public.${table}.${column}: HTTP ${res.status} ${body.message ?? ""}`.trim() };
}

async function checkRpc({ name, params }) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${name}`, {
    method: "POST",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify(params),
  });
  if (res.status === 200 || res.status === 206) return { ok: true };
  const body = await res.json().catch(() => ({}));
  if (body.code === "PGRST202") return { ok: false, reason: `Missing RPC: public.${name}(...)` };
  return { ok: false, reason: `Unexpected error checking RPC public.${name}: HTTP ${res.status} ${body.message ?? ""}`.trim() };
}

async function main() {
  const problems = [];

  for (const table of TABLES) {
    const result = await checkTable(table);
    if (!result.ok) problems.push(result.reason);
  }

  for (const [table, column] of CRITICAL_COLUMNS) {
    const result = await checkColumn(table, column);
    if (!result.ok) problems.push(result.reason);
  }

  for (const rpc of RPCS) {
    const result = await checkRpc(rpc);
    if (!result.ok) problems.push(result.reason);
  }

  console.log(`Verificado contra: ${SUPABASE_URL}`);
  console.log(`Tabelas checadas: ${TABLES.length} | Colunas críticas: ${CRITICAL_COLUMNS.length} | RPCs: ${RPCS.length}`);
  console.log("");

  if (problems.length === 0) {
    console.log("SCHEMA OK");
    process.exit(0);
  }

  console.log("SCHEMA DRIFT DETECTED");
  console.log("");
  for (const p of problems) console.log(p);
  process.exit(1);
}

main().catch((err) => {
  console.error("Não foi possível checar: erro de rede ou resposta inesperada do Supabase.");
  console.error(String(err?.message ?? err));
  process.exit(2);
});
