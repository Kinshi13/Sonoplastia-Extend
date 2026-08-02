"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, X, ChevronUp, ChevronDown } from "lucide-react";
import { OrganizationRole, ScaleTemplate } from "@/lib/types/database";
import { withLegacyRoleFallback } from "@/lib/legacyRoles";
import { saveScaleTemplateAction } from "../actions";

const inputClass = "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

/**
 * "Escalas padrão" (modelos) - same role add/remove/reorder UX as ScaleForm's "Funções e pessoas",
 * minus the PersonSelector: a template only ever carries role structure ({roleId, position}),
 * never people (Bloco: "modelos só guardam a estrutura, nunca as pessoas").
 */
export function ScaleTemplateForm({
  existing,
  churchId,
  roles,
}: {
  existing: ScaleTemplate | null;
  churchId: string;
  roles: OrganizationRole[];
}) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const effectiveRoles = useMemo(() => withLegacyRoleFallback(churchId, roles), [churchId, roles]);
  const [roleIds, setRoleIds] = useState<string[]>(() =>
    existing ? [...existing.roles].sort((a, b) => a.position - b.position).map((r) => r.roleId) : effectiveRoles.filter((r) => r.is_required).map((r) => r.id)
  );

  const usedRoleIds = new Set(roleIds);
  const availableRoles = effectiveRoles.filter((r) => r.is_active && !usedRoleIds.has(r.id));

  function addRole(roleId: string) {
    setRoleIds((prev) => [...prev, roleId]);
  }
  function removeRole(index: number) {
    setRoleIds((prev) => prev.filter((_, i) => i !== index));
  }
  function moveRole(index: number, dir: -1 | 1) {
    setRoleIds((prev) => {
      const next = [...prev];
      const target = index + dir;
      if (target < 0 || target >= next.length) return prev;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    formData.set("roles", JSON.stringify(roleIds.map((roleId, position) => ({ roleId, position }))));
    const result = await saveScaleTemplateAction(existing?.id ?? null, formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    router.push("/admin/escalas/modelos");
    router.refresh();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-4 max-w-xl">
      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Nome do modelo</span>
        <input name="name" required defaultValue={existing?.name} disabled={existing?.is_protected} className={inputClass} />
        {existing?.is_protected && (
          <span className="text-xs text-text-secondary">Este é um dos 3 modelos padrão - o nome não pode ser alterado, mas as funções e horários sim.</span>
        )}
      </label>

      <label className="flex flex-col gap-1">
        <span className="text-sm font-medium">Descrição (opcional)</span>
        <input name="description" defaultValue={existing?.description} className={inputClass} />
      </label>

      <div className="grid grid-cols-2 gap-3">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Início padrão (opcional)</span>
          <input type="time" name="default_start_time" defaultValue={existing?.default_start_time?.slice(0, 5) ?? ""} className={inputClass} />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Fim padrão (opcional)</span>
          <input type="time" name="default_end_time" defaultValue={existing?.default_end_time?.slice(0, 5) ?? ""} className={inputClass} />
        </label>
      </div>

      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium">Funções</p>
        {roleIds.length === 0 && (
          <p className="text-sm text-text-secondary">Nenhuma função adicionada ainda.</p>
        )}
        {roleIds.map((roleId, index) => {
          const role = effectiveRoles.find((r) => r.id === roleId);
          if (!role) return null;
          return (
            <div key={roleId} className="flex items-center justify-between gap-2 rounded-lg border border-divider p-3">
              <span className="text-sm font-medium">{role.name}</span>
              <div className="flex items-center gap-1">
                <button type="button" onClick={() => moveRole(index, -1)} disabled={index === 0} aria-label="Mover para cima" className="text-text-muted hover:text-foreground disabled:opacity-30">
                  <ChevronUp size={14} />
                </button>
                <button type="button" onClick={() => moveRole(index, 1)} disabled={index === roleIds.length - 1} aria-label="Mover para baixo" className="text-text-muted hover:text-foreground disabled:opacity-30">
                  <ChevronDown size={14} />
                </button>
                <button type="button" onClick={() => removeRole(index)} aria-label={`Remover função ${role.name} deste modelo`} className="text-text-muted hover:text-error">
                  <X size={14} />
                </button>
              </div>
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
        <span className="text-sm font-medium">Observações padrão (opcional)</span>
        <textarea name="default_notes" defaultValue={existing?.default_notes} rows={2} className={inputClass} />
      </label>

      {error && <p className="text-sm text-error">{error}</p>}

      <button type="submit" disabled={pending} className="self-start rounded-full bg-primary px-5 py-2 text-sm font-medium text-white disabled:opacity-60">
        {pending ? "Salvando..." : "Salvar modelo"}
      </button>
    </form>
  );
}
