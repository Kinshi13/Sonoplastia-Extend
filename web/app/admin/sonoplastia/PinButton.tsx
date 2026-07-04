"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { Pin, PinOff } from "lucide-react";
import { toggleSharedFilePinAction } from "../actions";

export function PinButton({ id, pinned }: { id: string; pinned: boolean }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() =>
        startTransition(async () => {
          await toggleSharedFilePinAction(id, !pinned);
          router.refresh();
        })
      }
      className={`flex items-center gap-1.5 text-sm font-medium disabled:opacity-50 ${pinned ? "text-primary" : "text-text-secondary"}`}
    >
      {pinned ? <Pin size={14} /> : <PinOff size={14} />}
      {pinned ? "Fixado" : "Fixar"}
    </button>
  );
}
