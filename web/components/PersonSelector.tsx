"use client";

import { useMemo, useState } from "react";
import { Search, Plus, X, Star } from "lucide-react";
import { OrganizationPerson } from "@/lib/types/database";

export type PersonSelection = { personId: string | null; customPersonName: string | null };

/**
 * Fase 11.8.4 (Parte 7-9): replaces a plain text input for "who's assigned" with a real picker
 * over the organization's people bank - search, recent (derived from `recentPersonIds`, which the
 * caller computes from recent ScaleAssignments), favorites first, quick "cadastrar pessoa" escape
 * hatch, and a manual-name fallback for a one-off helper who isn't in the bank yet.
 */
export function PersonSelector({
  people,
  teamPersonIds,
  recentPersonIds,
  selected,
  allowMultiple,
  onChange,
  onQuickAdd,
}: {
  people: OrganizationPerson[];
  /** People belonging to the team associated with this role, shown first (Parte 6). */
  teamPersonIds: Set<string>;
  recentPersonIds: string[];
  selected: PersonSelection[];
  allowMultiple: boolean;
  onChange: (next: PersonSelection[]) => void;
  onQuickAdd: () => void;
}) {
  const [query, setQuery] = useState("");
  const [manualMode, setManualMode] = useState(false);

  const ranked = useMemo(() => {
    const q = query.trim().toLowerCase();
    const filtered = people.filter((p) => !q || p.full_name.toLowerCase().includes(q) || p.display_name?.toLowerCase().includes(q));
    return [...filtered].sort((a, b) => {
      const aTeam = teamPersonIds.has(a.id), bTeam = teamPersonIds.has(b.id);
      if (aTeam !== bTeam) return aTeam ? -1 : 1;
      const aRecent = recentPersonIds.indexOf(a.id), bRecent = recentPersonIds.indexOf(b.id);
      const aRank = aRecent === -1 ? 999 : aRecent, bRank = bRecent === -1 ? 999 : bRecent;
      if (aRank !== bRank) return aRank - bRank;
      if (a.is_favorite !== b.is_favorite) return a.is_favorite ? -1 : 1;
      return a.full_name.localeCompare(b.full_name);
    });
  }, [people, query, teamPersonIds, recentPersonIds]);

  const selectedPersonIds = new Set(selected.map((s) => s.personId).filter(Boolean));

  function toggle(personId: string) {
    if (selectedPersonIds.has(personId)) {
      onChange(selected.filter((s) => s.personId !== personId));
      return;
    }
    const next = { personId, customPersonName: null };
    onChange(allowMultiple ? [...selected, next] : [next]);
  }

  function removeAt(index: number) {
    onChange(selected.filter((_, i) => i !== index));
  }

  function addManualName(name: string) {
    if (!name.trim()) return;
    const next = { personId: null, customPersonName: name.trim() };
    onChange(allowMultiple ? [...selected, next] : [next]);
    setManualMode(false);
  }

  return (
    <div className="flex flex-col gap-2">
      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {selected.map((s, i) => {
            const person = s.personId ? people.find((p) => p.id === s.personId) : null;
            const label = person ? person.display_name || person.full_name : s.customPersonName ?? "";
            return (
              <span
                key={i}
                className="flex items-center gap-1.5 rounded-full bg-primary-container px-3 py-1 text-xs font-medium text-on-primary-container"
              >
                {label}
                <button type="button" onClick={() => removeAt(i)} aria-label={`Remover ${label}`} className="hover:opacity-70">
                  <X size={12} />
                </button>
              </span>
            );
          })}
        </div>
      )}

      {(allowMultiple || selected.length === 0) && (
        <div className="rounded-lg border border-divider">
          <div className="relative border-b border-divider">
            <Search size={14} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-text-muted" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar pessoa..."
              className="w-full bg-transparent py-2 pl-8 pr-3 text-sm outline-none"
            />
          </div>
          <div className="max-h-40 overflow-y-auto p-1.5">
            {ranked.length === 0 ? (
              <p className="px-2 py-2 text-xs text-text-secondary">Nenhuma pessoa encontrada.</p>
            ) : (
              ranked.slice(0, 20).map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => toggle(p.id)}
                  className={`flex w-full items-center justify-between gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-primary-container/20 ${
                    selectedPersonIds.has(p.id) ? "bg-primary-container/30 font-medium" : ""
                  }`}
                >
                  <span className="truncate">{p.display_name || p.full_name}</span>
                  {p.is_favorite && <Star size={12} className="shrink-0 text-accent-star" fill="currentColor" />}
                </button>
              ))
            )}
          </div>
          <div className="flex items-center gap-2 border-t border-divider p-1.5">
            <button
              type="button"
              onClick={onQuickAdd}
              className="flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-primary hover:bg-primary-container/20"
            >
              <Plus size={12} /> Cadastrar pessoa
            </button>
            <button
              type="button"
              onClick={() => setManualMode((v) => !v)}
              className="ml-auto text-xs font-medium text-text-secondary hover:text-foreground"
            >
              Nome manual
            </button>
          </div>
          {manualMode && (
            <div className="flex items-center gap-1.5 border-t border-divider p-1.5">
              <input
                autoFocus
                placeholder="Digitar nome..."
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addManualName((e.target as HTMLInputElement).value);
                  }
                }}
                className="flex-1 rounded-md border border-divider bg-surface px-2 py-1 text-xs outline-none"
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
