"use client";

import { useTransition } from "react";

export function DeleteButton({ id, action }: { id: string; action: (id: string) => Promise<void> }) {
  const [isPending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={isPending}
      onClick={() => {
        if (!confirm("Tem certeza que deseja excluir?")) return;
        startTransition(() => action(id));
      }}
      className="text-sm text-error disabled:opacity-50"
    >
      Excluir
    </button>
  );
}
