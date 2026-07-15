"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Calendar, Search, RotateCcw } from "lucide-react";
import { Doxology } from "@/lib/types/database";
import { formatDatePt, formatTimePt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { FavoriteButton } from "../FavoriteButton";

/** Bloco P: favoritas primeiro, depois mais recentes, depois mais reutilizadas. */
function sortDoxologies(items: Doxology[]): Doxology[] {
  return [...items].sort((a, b) => {
    const aFav = a.is_favorite ?? false;
    const bFav = b.is_favorite ?? false;
    if (aFav !== bFav) return aFav ? -1 : 1;
    if (a.date !== b.date) return a.date < b.date ? 1 : -1;
    return (b.times_reused ?? 0) - (a.times_reused ?? 0);
  });
}

function matchesQuery(item: Doxology, query: string): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return true;
  if (item.title.toLowerCase().includes(q)) return true;
  if (item.date.includes(q)) return true;
  return item.program_order.some((step) => step.title.toLowerCase().includes(q));
}

export function ReuseDoxologyList({ items }: { items: Doxology[] }) {
  const [query, setQuery] = useState("");
  const today = new Date().toISOString().slice(0, 10);

  const filtered = useMemo(() => sortDoxologies(items).filter((item) => matchesQuery(item, query)), [items, query]);

  return (
    <div className="flex flex-col gap-4">
      <div className="relative max-w-sm">
        <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar por título, data ou etapa..."
          className="w-full rounded-full border border-border-soft bg-surface py-2 pl-9 pr-3 text-sm outline-none focus:border-border-focus"
        />
      </div>

      {filtered.length === 0 ? (
        <EmptyState message="Nenhuma programação encontrada para essa busca." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((item) => (
            <Card key={item.id} className="p-5 flex flex-col gap-3">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-semibold leading-tight truncate">{item.title}</p>
                  <p className="mt-1 flex items-center gap-1.5 text-sm text-text-secondary capitalize">
                    <Calendar size={14} className="shrink-0" />
                    {formatDatePt(item.date)} às {formatTimePt(item.start_time)}
                  </p>
                </div>
                <FavoriteButton id={item.id} initial={item.is_favorite ?? false} />
              </div>

              <p className="text-xs text-text-secondary">
                {item.program_order.length} {item.program_order.length === 1 ? "etapa" : "etapas"} ·{" "}
                {item.date < today ? "Concluída" : "Futura"}
                {(item.times_reused ?? 0) > 0 && <> · Reutilizada {item.times_reused}x</>}
              </p>

              {item.program_order.length > 0 && (
                <ul className="text-xs text-text-secondary flex flex-col gap-0.5">
                  {item.program_order.slice(0, 3).map((step, i) => (
                    <li key={i} className="truncate">
                      {step.order}. {step.title || "(sem título)"}
                    </li>
                  ))}
                  {item.program_order.length > 3 && <li>+ {item.program_order.length - 3} outras</li>}
                </ul>
              )}

              <Link
                href={`/admin/doxologia/reutilizar/${item.id}`}
                className="mt-auto flex items-center justify-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white hover:opacity-90 transition-opacity"
              >
                <RotateCcw size={14} /> Usar esta
              </Link>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
