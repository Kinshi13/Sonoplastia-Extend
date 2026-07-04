"use client";

import { useState } from "react";
import { startChurchSignupAction } from "./actions";

export default function CadastrarIgrejaPage() {
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [slug, setSlug] = useState("");

  async function handleSubmit(formData: FormData) {
    setPending(true);
    setError(null);
    const result = await startChurchSignupAction(formData);
    if (result.error) {
      setError(result.error);
      setPending(false);
      return;
    }
    if (result.checkoutUrl) window.location.href = result.checkoutUrl;
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="text-2xl font-semibold mb-1">Cadastrar igreja</h1>
      <p className="text-sm text-text-secondary mb-6">
        Pagamento único para ativar o painel administrativo da sua igreja. Depois de confirmado, o
        login abaixo vira o administrador.
      </p>

      <form action={handleSubmit} className="flex flex-col gap-4">
        <Field label="Nome da igreja">
          <input name="church_name" required className={inputClass} />
        </Field>

        <Field label="Código da igreja (aparece no link público)">
          <input
            name="slug"
            required
            value={slug}
            onChange={(e) => setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, "-"))}
            placeholder="igreja-central"
            className={inputClass}
          />
          <p className="mt-1 text-xs text-text-secondary">
            Link público: seusite.com/c/{slug || "codigo-da-igreja"}
          </p>
        </Field>

        <Field label="Seu e-mail (login de administrador)">
          <input type="email" name="email" required className={inputClass} />
        </Field>

        <Field label="Senha">
          <input type="password" name="password" required minLength={6} className={inputClass} />
        </Field>

        {error && <p className="text-sm text-error">{error}</p>}

        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
        >
          {pending ? "Redirecionando para pagamento..." : "Continuar para o pagamento"}
        </button>
      </form>
    </div>
  );
}

const inputClass =
  "w-full rounded-lg border border-divider bg-surface px-3 py-2 text-sm outline-none focus:border-primary";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{label}</span>
      {children}
    </label>
  );
}
