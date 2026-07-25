"use client";

import { useTransition } from "react";
import { ChevronUp, ChevronDown } from "lucide-react";
import { moveWorshipSongAction } from "../actions";

/** Música e Louvor (Bloco 14) - simple up/down reorder, deliberately not drag-and-drop ("não
 *  implementar drag-and-drop se isso atrasar ou complicar mobile"). */
export function ReorderButtons({ id }: { id: string }) {
  const [isPending, startTransition] = useTransition();

  function move(direction: "up" | "down") {
    startTransition(() => {
      moveWorshipSongAction(id, direction);
    });
  }

  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        disabled={isPending}
        onClick={() => move("up")}
        aria-label="Mover música para cima"
        className="rounded p-1 text-text-secondary hover:text-primary disabled:opacity-50"
      >
        <ChevronUp size={16} />
      </button>
      <button
        type="button"
        disabled={isPending}
        onClick={() => move("down")}
        aria-label="Mover música para baixo"
        className="rounded p-1 text-text-secondary hover:text-primary disabled:opacity-50"
      >
        <ChevronDown size={16} />
      </button>
    </div>
  );
}
