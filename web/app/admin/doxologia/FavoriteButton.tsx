"use client";

import { useState } from "react";
import { Star } from "lucide-react";
import { toggleDoxologyFavoriteAction } from "../actions";

/** Bloco S: favoriting a Doxologia so it sorts first in "Reutilizar programação recente". */
export function FavoriteButton({ id, initial }: { id: string; initial: boolean }) {
  const [favorite, setFavorite] = useState(initial);
  const [pending, setPending] = useState(false);

  async function toggle() {
    const next = !favorite;
    setFavorite(next);
    setPending(true);
    const result = await toggleDoxologyFavoriteAction(id, next);
    setPending(false);
    if (result.error) setFavorite(!next);
  }

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={pending}
      aria-pressed={favorite}
      aria-label={favorite ? "Remover dos favoritos" : "Marcar como favorita"}
      className="shrink-0 rounded-full p-1.5 text-text-muted transition-colors hover:text-accent-star disabled:opacity-60"
      style={favorite ? { color: "var(--accent-star)" } : undefined}
    >
      <Star size={16} fill={favorite ? "currentColor" : "none"} />
    </button>
  );
}
