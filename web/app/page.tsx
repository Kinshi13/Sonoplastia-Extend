"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function ChurchPickerPage() {
  const router = useRouter();
  const [code, setCode] = useState("");

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    const slug = code.trim().toLowerCase().replace(/\s+/g, "-");
    if (slug) router.push(`/c/${slug}`);
  }

  return (
    <div className="mx-auto max-w-sm text-center">
      <h1 className="text-2xl font-bold tracking-tight mb-1">Escala Church</h1>
      <p className="text-sm text-text-secondary mb-6">
        Digite o código da sua igreja para ver a escala, doxologia, anúncios e retrospectiva.
      </p>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="ex: igreja-central"
          required
          className="w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm text-center outline-none focus:border-primary"
        />
        <button
          type="submit"
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          Acessar
        </button>
      </form>

      <p className="mt-8 text-xs text-text-secondary">
        É administrador e sua igreja ainda não está cadastrada?{" "}
        <Link href="/cadastrar-igreja" className="text-primary font-medium">
          Cadastre aqui
        </Link>
        .
      </p>
    </div>
  );
}
