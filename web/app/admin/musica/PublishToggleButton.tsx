"use client";

import { useTransition } from "react";
import { toggleWorshipSongPublishedAction } from "../actions";

/** Música e Louvor - "Publicar"/"Despublicar" straight from the list card, same pattern as
 *  admin/anuncios/PublishToggleButton (native confirm, no separate dialog component). */
export function PublishToggleButton({ id, isPublished }: { id: string; isPublished: boolean }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() => {
        const message = isPublished
          ? "Despublicar esta música? Ela deixará de aparecer para os membros."
          : "Publicar esta música? Ela passará a aparecer para os membros.";
        if (!confirm(message)) return;
        startTransition(() => {
          toggleWorshipSongPublishedAction(id, !isPublished);
        });
      }}
      className="text-sm font-medium text-primary disabled:opacity-50"
    >
      {isPublished ? "Despublicar" : "Publicar"}
    </button>
  );
}
