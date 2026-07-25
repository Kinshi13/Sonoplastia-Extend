"use client";

import { useTransition } from "react";
import { toggleAnnouncementActiveAction } from "../actions";

/** Usabilidade (edição de anúncios) - "Publicar"/"Despublicar" straight from the list card, same
 *  pattern as DeleteButton (native confirm, no separate dialog component). */
export function PublishToggleButton({ id, isActive }: { id: string; isActive: boolean }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() => {
        const message = isActive
          ? "Despublicar este anúncio? Ele deixará de aparecer para os membros."
          : "Publicar este anúncio? Ele passará a aparecer para os membros.";
        if (!confirm(message)) return;
        startTransition(() => {
          toggleAnnouncementActiveAction(id, !isActive);
        });
      }}
      className="text-sm font-medium text-primary disabled:opacity-50"
    >
      {isActive ? "Despublicar" : "Publicar"}
    </button>
  );
}
