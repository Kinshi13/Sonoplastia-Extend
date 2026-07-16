"use client";

import { useState } from "react";
import { Plus, Star, Archive, Trash2, Users, Shield } from "lucide-react";
import { OrganizationPerson, OrganizationRole, OrganizationTeam, PersonTeamMembership } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import {
  saveRoleAction,
  setRoleActiveAction,
  deleteRoleAction,
  savePersonAction,
  setPersonFavoriteAction,
  setPersonActiveAction,
  deletePersonAction,
  saveTeamAction,
  deleteTeamAction,
} from "./actions";

type Tab = "people" | "teams" | "roles";
const inputClass = "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

export function PeopleTeamsManager({
  people,
  teams,
  roles,
  memberships,
}: {
  people: OrganizationPerson[];
  teams: OrganizationTeam[];
  roles: OrganizationRole[];
  memberships: PersonTeamMembership[];
}) {
  const [tab, setTab] = useState<Tab>("people");

  return (
    <div>
      <div className="mb-5 flex gap-1 border-b border-divider">
        <TabButton active={tab === "people"} onClick={() => setTab("people")} icon={Users} label={`Pessoas (${people.length})`} />
        <TabButton active={tab === "teams"} onClick={() => setTab("teams")} icon={Shield} label={`Equipes (${teams.length})`} />
        <TabButton active={tab === "roles"} onClick={() => setTab("roles")} icon={Star} label={`Funções (${roles.length})`} />
      </div>

      {tab === "people" && <PeopleTab people={people} teams={teams} memberships={memberships} />}
      {tab === "teams" && <TeamsTab teams={teams} />}
      {tab === "roles" && <RolesTab roles={roles} teams={teams} />}
    </div>
  );
}

function TabButton({ active, onClick, icon: Icon, label }: { active: boolean; onClick: () => void; icon: typeof Users; label: string }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition-colors ${
        active ? "border-primary text-primary" : "border-transparent text-text-secondary hover:text-foreground"
      }`}
    >
      <Icon size={15} /> {label}
    </button>
  );
}

// Pessoas --------------------------------------------------------------------

function PeopleTab({ people, teams, memberships }: { people: OrganizationPerson[]; teams: OrganizationTeam[]; memberships: PersonTeamMembership[] }) {
  const [editing, setEditing] = useState<OrganizationPerson | "new" | null>(null);

  return (
    <div className="flex flex-col gap-4">
      <button
        onClick={() => setEditing("new")}
        className="flex w-fit items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
      >
        <Plus size={16} /> Nova pessoa
      </button>

      {editing && (
        <PersonForm
          existing={editing === "new" ? null : editing}
          teams={teams}
          memberships={memberships}
          onDone={() => setEditing(null)}
        />
      )}

      {people.length === 0 ? (
        <EmptyState message="Nenhuma pessoa cadastrada ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {people.map((p) => {
            const personTeams = memberships.filter((m) => m.person_id === p.id).map((m) => teams.find((t) => t.id === m.team_id)?.name).filter(Boolean);
            return (
              <Card key={p.id} className={`p-4 flex flex-col gap-2 ${!p.is_active ? "opacity-60" : ""}`}>
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-medium leading-tight truncate">{p.full_name}</p>
                    {personTeams.length > 0 && <p className="text-xs text-text-secondary truncate">{personTeams.join(", ")}</p>}
                  </div>
                  <FavoriteToggle id={p.id} initial={p.is_favorite} />
                </div>
                <div className="mt-auto flex items-center gap-3 border-t border-divider pt-2 text-xs">
                  <button onClick={() => setEditing(p)} className="font-medium text-primary">Editar</button>
                  <ArchiveToggle id={p.id} isActive={p.is_active} onSet={setPersonActiveAction} />
                  <DeletePersonButton id={p.id} />
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}

function PersonForm({
  existing,
  teams,
  memberships,
  onDone,
}: {
  existing: OrganizationPerson | null;
  teams: OrganizationTeam[];
  memberships: PersonTeamMembership[];
  onDone: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const currentTeamIds = new Set(memberships.filter((m) => m.person_id === existing?.id).map((m) => m.team_id));

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await savePersonAction(existing?.id ?? null, formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    onDone();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-3 rounded-lg border border-divider p-4 max-w-md">
      <input name="full_name" required defaultValue={existing?.full_name} placeholder="Nome completo" className={inputClass} />
      <input name="display_name" defaultValue={existing?.display_name ?? ""} placeholder="Como aparece na escala (opcional)" className={inputClass} />
      <div className="grid grid-cols-2 gap-2">
        <input name="email" type="email" defaultValue={existing?.email ?? ""} placeholder="E-mail (opcional)" className={inputClass} />
        <input name="phone" defaultValue={existing?.phone ?? ""} placeholder="Telefone (opcional)" className={inputClass} />
      </div>
      {teams.length > 0 && (
        <div>
          <p className="text-xs font-medium text-text-secondary mb-1">Equipes</p>
          <div className="flex flex-wrap gap-2">
            {teams.map((t) => (
              <label key={t.id} className="flex items-center gap-1.5 rounded-full border border-divider px-2.5 py-1 text-xs">
                <input type="checkbox" name="team_ids" value={t.id} defaultChecked={currentTeamIds.has(t.id)} />
                {t.name}
              </label>
            ))}
          </div>
        </div>
      )}
      <textarea name="notes" defaultValue={existing?.notes} placeholder="Observações (opcional)" rows={2} className={inputClass} />
      {error && <p className="text-sm text-error">{error}</p>}
      <div className="flex items-center gap-2">
        <button type="submit" disabled={pending} className="rounded-full bg-primary px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60">
          {pending ? "Salvando..." : "Salvar"}
        </button>
        <button type="button" onClick={onDone} className="text-sm text-text-secondary">Cancelar</button>
      </div>
    </form>
  );
}

function FavoriteToggle({ id, initial }: { id: string; initial: boolean }) {
  const [favorite, setFavorite] = useState(initial);
  return (
    <button
      onClick={async () => {
        setFavorite(!favorite);
        const result = await setPersonFavoriteAction(id, !favorite);
        if (result.error) setFavorite(favorite);
      }}
      aria-label={favorite ? "Remover dos favoritos" : "Marcar como favorito"}
      className="shrink-0 text-text-muted hover:text-accent-star"
      style={favorite ? { color: "var(--accent-star)" } : undefined}
    >
      <Star size={15} fill={favorite ? "currentColor" : "none"} />
    </button>
  );
}

function ArchiveToggle({ id, isActive, onSet }: { id: string; isActive: boolean; onSet: (id: string, next: boolean) => Promise<{ error?: string }> }) {
  const [active, setActive] = useState(isActive);
  return (
    <button
      onClick={async () => {
        setActive(!active);
        const result = await onSet(id, !active);
        if (result.error) setActive(active);
      }}
      className="flex items-center gap-1 font-medium text-text-secondary hover:text-foreground"
    >
      <Archive size={12} /> {active ? "Arquivar" : "Reativar"}
    </button>
  );
}

function DeletePersonButton({ id }: { id: string }) {
  const [error, setError] = useState<string | null>(null);
  return (
    <span className="ml-auto">
      <button
        onClick={async () => {
          if (!confirm("Excluir esta pessoa definitivamente?")) return;
          const result = await deletePersonAction(id);
          if (result.error) setError(result.error);
        }}
        className="flex items-center gap-1 font-medium text-error"
      >
        <Trash2 size={12} /> Excluir
      </button>
      {error && <p className="text-error mt-1">{error}</p>}
    </span>
  );
}

// Equipes --------------------------------------------------------------------

function TeamsTab({ teams }: { teams: OrganizationTeam[] }) {
  const [editing, setEditing] = useState<OrganizationTeam | "new" | null>(null);

  return (
    <div className="flex flex-col gap-4">
      <button onClick={() => setEditing("new")} className="flex w-fit items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white">
        <Plus size={16} /> Nova equipe
      </button>
      {editing && <TeamForm existing={editing === "new" ? null : editing} onDone={() => setEditing(null)} />}
      {teams.length === 0 ? (
        <EmptyState message="Nenhuma equipe cadastrada ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {teams.map((t) => (
            <Card key={t.id} className="p-4 flex flex-col gap-2">
              <p className="font-medium">{t.name}</p>
              {t.description && <p className="text-xs text-text-secondary">{t.description}</p>}
              <div className="mt-auto flex items-center gap-3 border-t border-divider pt-2 text-xs">
                <button onClick={() => setEditing(t)} className="font-medium text-primary">Editar</button>
                <button
                  onClick={async () => {
                    if (!confirm("Excluir esta equipe?")) return;
                    await deleteTeamAction(t.id);
                  }}
                  className="flex items-center gap-1 font-medium text-error"
                >
                  <Trash2 size={12} /> Excluir
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function TeamForm({ existing, onDone }: { existing: OrganizationTeam | null; onDone: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await saveTeamAction(existing?.id ?? null, formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    onDone();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-3 rounded-lg border border-divider p-4 max-w-md">
      <input name="name" required defaultValue={existing?.name} placeholder="Nome da equipe" className={inputClass} />
      <textarea name="description" defaultValue={existing?.description} placeholder="Descrição (opcional)" rows={2} className={inputClass} />
      {error && <p className="text-sm text-error">{error}</p>}
      <div className="flex items-center gap-2">
        <button type="submit" disabled={pending} className="rounded-full bg-primary px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60">
          {pending ? "Salvando..." : "Salvar"}
        </button>
        <button type="button" onClick={onDone} className="text-sm text-text-secondary">Cancelar</button>
      </div>
    </form>
  );
}

// Funções --------------------------------------------------------------------

function RolesTab({ roles, teams }: { roles: OrganizationRole[]; teams: OrganizationTeam[] }) {
  const [editing, setEditing] = useState<OrganizationRole | "new" | null>(null);

  return (
    <div className="flex flex-col gap-4">
      <button onClick={() => setEditing("new")} className="flex w-fit items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white">
        <Plus size={16} /> Nova função
      </button>
      {editing && <RoleForm existing={editing === "new" ? null : editing} teams={teams} onDone={() => setEditing(null)} />}
      {roles.length === 0 ? (
        <EmptyState message="Nenhuma função cadastrada ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {roles.map((r) => (
            <Card key={r.id} className={`p-4 flex flex-col gap-2 ${!r.is_active ? "opacity-60" : ""}`}>
              <div className="flex items-start justify-between gap-2">
                <p className="font-medium leading-tight">{r.name}</p>
                {r.is_required && <span className="shrink-0 rounded-full bg-primary-container px-2 py-0.5 text-[10px] font-medium text-on-primary-container">Padrão</span>}
              </div>
              {r.description && <p className="text-xs text-text-secondary">{r.description}</p>}
              {r.allows_multiple_people && <p className="text-xs text-text-secondary">Permite várias pessoas</p>}
              <div className="mt-auto flex items-center gap-3 border-t border-divider pt-2 text-xs">
                <button onClick={() => setEditing(r)} className="font-medium text-primary">Editar</button>
                <ArchiveToggle id={r.id} isActive={r.is_active} onSet={setRoleActiveAction} />
                {!r.is_required && <DeleteRoleButton id={r.id} />}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

function DeleteRoleButton({ id }: { id: string }) {
  const [error, setError] = useState<string | null>(null);
  return (
    <span className="ml-auto">
      <button
        onClick={async () => {
          if (!confirm("Excluir esta função definitivamente?")) return;
          const result = await deleteRoleAction(id);
          if (result.error) setError(result.error);
        }}
        className="flex items-center gap-1 font-medium text-error"
      >
        <Trash2 size={12} /> Excluir
      </button>
      {error && <p className="text-error mt-1">{error}</p>}
    </span>
  );
}

function RoleForm({ existing, teams, onDone }: { existing: OrganizationRole | null; teams: OrganizationTeam[]; onDone: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await saveRoleAction(existing?.id ?? null, formData);
    setPending(false);
    if (result.error) {
      setError(result.error);
      return;
    }
    onDone();
  }

  return (
    <form action={handleSubmit} className="flex flex-col gap-3 rounded-lg border border-divider p-4 max-w-md">
      <input name="name" required defaultValue={existing?.name} placeholder="Nome da função" className={inputClass} />
      <textarea name="description" defaultValue={existing?.description} placeholder="Descrição (opcional)" rows={2} className={inputClass} />
      {teams.length > 0 && (
        <label className="flex flex-col gap-1">
          <span className="text-xs font-medium text-text-secondary">Equipe (opcional)</span>
          <select name="team_id" defaultValue={existing?.team_id ?? ""} className={inputClass}>
            <option value="">Nenhuma</option>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </label>
      )}
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="allows_multiple_people" defaultChecked={existing?.allows_multiple_people} />
        Permite selecionar mais de uma pessoa
      </label>
      {error && <p className="text-sm text-error">{error}</p>}
      <div className="flex items-center gap-2">
        <button type="submit" disabled={pending} className="rounded-full bg-primary px-4 py-1.5 text-sm font-medium text-white disabled:opacity-60">
          {pending ? "Salvando..." : "Salvar"}
        </button>
        <button type="button" onClick={onDone} className="text-sm text-text-secondary">Cancelar</button>
      </div>
    </form>
  );
}
