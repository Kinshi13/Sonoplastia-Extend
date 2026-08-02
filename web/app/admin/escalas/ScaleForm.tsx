"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, X, ChevronUp, ChevronDown } from "lucide-react";
import { Scale, OrganizationRole, OrganizationPerson, PersonTeamMembership, ScaleAssignment } from "@/lib/types/database";
import { PersonSelector, PersonSelection } from "@/components/PersonSelector";
import { withLegacyRoleFallback, legacyRowsFromScale } from "@/lib/legacyRoles";
import { saveScaleAction } from "../actions";
import { savePersonAction } from "../pessoas/actions";

type Row = { roleId: string; selections: PersonSelection[] };

const inputClass = "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

/**
 * Fase 11.8.4 (Parte 2, 7, 10): the five fixed text inputs are gone - the form now lists whatever
 * `OrganizationRole`s exist for this church (Parte 3's migration seeds the original five so
 * nothing regresses on day one), each with a real PersonSelector instead of free text. Roles can
 * be added/removed/reordered per scale (Parte 10) without touching the organization's role list.
 *
 * Hotfix: `roles`/`existingAssignments` can legitimately come back empty even when the scale has
 * real data - migrations 006-010 might not be applied yet (`organization_roles`/
 * `scale_assignments` don't exist), or a scale was saved before either existed. `withLegacyRoleFallback`
 * fills in the five original roles client-side when the DB doesn't have them, and when editing an
 * existing scale with no assignments, its own legacy columns (reception_person, etc.) seed the
 * rows directly - "Funções e pessoas" should never just be silently empty.
 */
export function ScaleForm({
  existing,
  churchId,
  roles,
  initialPeople,
  memberships,
  existingAssignments,
  recentPersonIds,
}: {
  existing: Scale | null;
  churchId: string;
  roles: OrganizationRole[];
  initialPeople: OrganizationPerson[];
  memberships: PersonTeamMembership[];
  existingAssignments: ScaleAssignment[];
  recentPersonIds: string[];
}) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [people, setPeople] = useState(initialPeople);
  const [quickAddRoleIndex, setQuickAddRoleIndex] = useState<number | null>(null);

  const effectiveRoles = useMemo(() => withLegacyRoleFallback(churchId, roles), [churchId, roles]);
  const roleByLegacyField = useMemo(() => {
    const map = new Map<string, OrganizationRole>();
    for (const r of effectiveRoles) if (r.legacy_field_key) map.set(r.legacy_field_key, r);
    return map;
  }, [effectiveRoles]);

  const [rows, setRows] = useState<Row[]>(() => {
    if (existingAssignments.length > 0) {
      const byRole = new Map<string, PersonSelection[]>();
      for (const a of [...existingAssignments].sort((x, y) => x.position - y.position)) {
        const list = byRole.get(a.role_id) ?? [];
        list.push({ personId: a.person_id, customPersonName: a.custom_person_name });
        byRole.set(a.role_id, list);
      }
      return Array.from(byRole.entries()).map(([roleId, selections]) => ({ roleId, selections }));
    }
    // Editing an old scale that predates scale_assignments entirely: its legacy columns are the
    // only place the data lives - convert each filled-in one into its own editable row.
    if (existing) {
      const legacyRows = legacyRowsFromScale(existing);
      if (legacyRows.length > 0) {
        return legacyRows.map(({ field }) => {
          const role = roleByLegacyField.get(field);
          return { roleId: role?.id ?? field, selections: [{ personId: null, customPersonName: existing[field] }] };
        });
      }
    }
    // New scale (or an old one with every legacy field blank): pre-populate every required role
    // so nothing that used to always show up on the form vanishes.
    return effectiveRoles.filter((r) => r.is_required && r.is_active).map((r) => ({ roleId: r.id, selections: [] }));
  });

  const usedRoleIds = new Set(rows.map((r) => r.roleId));
  const availableRoles = effectiveRoles.filter((r) => r.is_active && !usedRoleIds.has(r.id));

  function addRole(roleId: string) {
    setRows((prev) => [...prev, { roleId, selections: [] }]);
  }
  function removeRow(index: number) {
    setRows((prev) => prev.filter((_, i) => i !== index));
  }
  function moveRow(index: number, dir: -1 | 1) {
    setRows((prev) => {
      const next = [...prev];
      const target = index + dir;
      if (target < 0 || target >= next.length) return prev;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }
  function setSelections(index: number, selections: PersonSelection[]) {
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, selections } : r)));
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const assignments = rows.flatMap((row, rowIndex) =>
      row.selections.map((s, i) => ({ roleId: row.roleId, personId: s.personId, customPersonName: s.customPersonName, position: rowIndex * 100 + i, notes: "" }))
    );
    formData.set("assignments", JSON.stringify(assignments));
    const result = await saveScaleAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/escalas");
    router.refresh();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Título</span>
        <input name="title" required defaultValue={existing?.title} className={inputClass} />
      </label>

      <div className="grid grid-cols-3 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Data</span>
          <input type="date" name="date" required defaultValue={existing?.date} className={inputClass} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Início</span>
          <input type="time" name="start_time" required defaultValue={existing?.start_time?.slice(0, 5)} className={inputClass} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Fim (opcional)</span>
          <input type="time" name="end_time" defaultValue={existing?.end_time?.slice(0, 5) ?? ""} className={inputClass} />
        </label>
      </div>

      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium">Funções e pessoas</p>
        {rows.length === 0 && (
          <p className="text-sm text-text-secondary">
            Nenhuma função adicionada ainda. Use &quot;Adicionar função&quot; abaixo para começar.
          </p>
        )}
        {rows.map((row, index) => {
          const role = effectiveRoles.find((r) => r.id === row.roleId);
          if (!role) return null;
          const teamPersonIds = new Set(role.team_id ? memberships.filter((m) => m.team_id === role.team_id).map((m) => m.person_id) : []);
          return (
            <div key={row.roleId} className="rounded-lg border border-divider p-3 flex flex-col gap-2">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium">{role.name}</span>
                <div className="flex items-center gap-1">
                  <button type="button" onClick={() => moveRow(index, -1)} disabled={index === 0} aria-label="Mover para cima" className="text-text-muted hover:text-foreground disabled:opacity-30">
                    <ChevronUp size={14} />
                  </button>
                  <button type="button" onClick={() => moveRow(index, 1)} disabled={index === rows.length - 1} aria-label="Mover para baixo" className="text-text-muted hover:text-foreground disabled:opacity-30">
                    <ChevronDown size={14} />
                  </button>
                  <button type="button" onClick={() => removeRow(index)} aria-label={`Remover função ${role.name} desta escala`} className="text-text-muted hover:text-error">
                    <X size={14} />
                  </button>
                </div>
              </div>
              <PersonSelector
                people={people}
                teamPersonIds={teamPersonIds}
                recentPersonIds={recentPersonIds}
                selected={row.selections}
                allowMultiple={role.allows_multiple_people}
                onChange={(next) => setSelections(index, next)}
                onQuickAdd={() => setQuickAddRoleIndex(index)}
              />
              {quickAddRoleIndex === index && (
                <QuickAddPerson
                  onDone={(person) => {
                    setPeople((prev) => [...prev, person]);
                    setSelections(index, role.allows_multiple_people ? [...row.selections, { personId: person.id, customPersonName: null }] : [{ personId: person.id, customPersonName: null }]);
                    setQuickAddRoleIndex(null);
                  }}
                  onCancel={() => setQuickAddRoleIndex(null)}
                />
              )}
            </div>
          );
        })}

        {availableRoles.length > 0 && (
          <label className="flex items-center gap-2 text-sm">
            <Plus size={14} className="text-primary" />
            <select
              value=""
              onChange={(e) => {
                if (e.target.value) addRole(e.target.value);
              }}
              className="rounded-lg border border-divider bg-surface px-2 py-1.5 text-sm"
            >
              <option value="">Adicionar função...</option>
              {availableRoles.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
          </label>
        )}
      </div>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Observações</span>
        <textarea name="notes" defaultValue={existing?.notes} rows={3} className={inputClass} />
      </label>

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="is_special_event" defaultChecked={existing?.is_special_event} />
        Evento especial
      </label>

      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" name="is_temporary" className="mt-0.5" defaultChecked={existing?.is_temporary} />
        <span>
          Escala temporária
          <span className="block text-xs text-text-secondary">
            Fora do ciclo oficial normal - para uma escala montada às pressas, sem seguir o padrão.
          </span>
        </span>
      </label>

      {error && <p className="text-sm text-error">{error}</p>}

      <button type="submit" disabled={pending} className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60">
        {pending ? "Salvando..." : "Salvar"}
      </button>
    </form>
  );
}

/**
 * Hotfix: this used to be its own nested `<form action={handleSubmit}>` *inside* ScaleForm's own
 * `<form>` - invalid HTML (forms can't nest), and with React 19's `action` prop specifically, a
 * submit dispatched on the inner form is not reliably contained to it: the outer ScaleForm could
 * end up submitting too, on stale/incomplete state, which is consistent with "cliquei em
 * Adicionar mas a escala parece não salvar". Rebuilt as a plain div with an explicit button
 * onClick instead of a second form element, so there is exactly one <form> on the page.
 */
function QuickAddPerson({ onDone, onCancel }: { onDone: (person: OrganizationPerson) => void; onCancel: () => void }) {
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleAdd() {
    const fullName = name.trim();
    if (!fullName) {
      setError("Informe o nome.");
      return;
    }
    setPending(true);
    setError(null);
    const formData = new FormData();
    formData.set("full_name", fullName);
    const result = await savePersonAction(null, formData);
    setPending(false);
    if (result.error || !result.id) {
      setError(result.error ?? "Não foi possível cadastrar a pessoa.");
      return;
    }
    onDone({
      id: result.id,
      church_id: "",
      full_name: fullName,
      display_name: null,
      email: null,
      phone: null,
      photo_url: null,
      notes: "",
      is_favorite: false,
      is_active: true,
      created_at: Date.now(),
      updated_at: Date.now(),
      created_by: null,
      linked_user_id: null,
    });
  }

  return (
    <div className="flex items-center gap-2 rounded-md border border-dashed border-divider p-2">
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            handleAdd();
          }
        }}
        autoFocus
        placeholder="Nome da nova pessoa"
        className={`${inputClass} py-1.5`}
      />
      <button type="button" onClick={handleAdd} disabled={pending} className="shrink-0 rounded-full bg-primary px-3 py-1.5 text-xs font-medium text-white disabled:opacity-60">
        {pending ? "..." : "Adicionar"}
      </button>
      <button type="button" onClick={onCancel} className="shrink-0 text-xs text-text-secondary">Cancelar</button>
      {error && <p className="text-xs text-error">{error}</p>}
    </div>
  );
}
