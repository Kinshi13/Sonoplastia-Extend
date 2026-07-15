"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Star, ArrowRight, X } from "lucide-react";
import { RecentChurch, getRecentChurches, forgetChurch } from "@/lib/recentChurches";

/**
 * Fase 11.8.2 (Bloco D): "Celestial Entry" - replaces the plain generic form with the same visual
 * language as the rest of the product (Constellation Calm background from the root layout, a
 * four-point star, Marcellus title). Bloco C's recent-churches list sits above the code field so a
 * returning visitor usually never has to type anything at all.
 */
export default function ChurchPickerPage() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [recent, setRecent] = useState<RecentChurch[]>([]);
  const [showCodeField, setShowCodeField] = useState(false);

  useEffect(() => {
    // One-time check against a browser-only API (localStorage) after mount - exactly what an
    // effect is for, not a derived-state cascade (same pattern as IosInstallHint).
    const list = getRecentChurches();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setRecent(list);
    setShowCodeField(list.length === 0);
  }, []);

  function goToSlug(slug: string) {
    const normalized = slug.trim().toLowerCase().replace(/\s+/g, "-");
    if (normalized) router.push(`/c/${normalized}`);
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    goToSlug(code);
  }

  function handleForget(churchId: string) {
    forgetChurch(churchId);
    setRecent(getRecentChurches());
  }

  return (
    <div className="mx-auto flex max-w-sm flex-col items-center gap-8 py-6 text-center sm:py-12">
      <div className="flex flex-col items-center gap-3">
        <span
          aria-hidden="true"
          className="flex h-14 w-14 items-center justify-center rounded-full border"
          style={{ background: "var(--cc-nebula)", borderColor: "var(--cc-horizon)", boxShadow: "var(--elevation-raised)" }}
        >
          <Star size={22} className="text-primary" fill="currentColor" strokeWidth={1} />
        </span>
        <div>
          <h1 className="font-display text-2xl tracking-tight">Escala Church</h1>
          <p className="mt-1 text-sm text-text-secondary">
            Escala, doxologia, anúncios e retrospectiva da sua igreja, em um só lugar.
          </p>
        </div>
      </div>

      {recent.length > 0 && (
        <div className="flex w-full flex-col gap-2.5 text-left">
          <p className="text-xs font-semibold uppercase tracking-[0.1em] text-text-muted">Continuar em</p>
          <ul className="flex flex-col gap-2">
            {recent.map((church) => (
              <li key={church.churchId}>
                <RecentChurchRow church={church} onGo={() => goToSlug(church.slug)} onForget={() => handleForget(church.churchId)} />
              </li>
            ))}
          </ul>
        </div>
      )}

      {showCodeField ? (
        <form onSubmit={handleSubmit} className="flex w-full flex-col gap-3">
          <div className="text-left">
            <label htmlFor="church-code" className="mb-1 block text-xs font-medium text-text-secondary">
              Código da igreja
            </label>
            <input
              id="church-code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="ex: igreja-central"
              required
              autoFocus={recent.length === 0}
              className="w-full rounded-[var(--radius-md)] border border-border-soft bg-surface px-4 py-3 text-sm outline-none transition-colors focus:border-border-focus"
              style={{ boxShadow: "var(--elevation-flat)" }}
            />
          </div>
          <button
            type="submit"
            className="flex items-center justify-center gap-1.5 rounded-full bg-primary px-4 py-3 text-sm font-medium text-white transition-opacity hover:opacity-90"
            style={{ boxShadow: "var(--elevation-raised)" }}
          >
            Entrar <ArrowRight size={16} />
          </button>
        </form>
      ) : (
        <button
          onClick={() => setShowCodeField(true)}
          className="text-sm font-medium text-primary hover:underline"
        >
          Usar outra igreja
        </button>
      )}

      <p className="text-xs text-text-secondary">
        É administrador e sua igreja ainda não está cadastrada?{" "}
        <Link href="/cadastrar-igreja" className="font-medium text-primary">
          Cadastre aqui
        </Link>
        .
      </p>
    </div>
  );
}

function RecentChurchRow({
  church,
  onGo,
  onForget,
}: {
  church: RecentChurch;
  onGo: () => void;
  onForget: () => void;
}) {
  return (
    <div
      className="group flex items-center gap-3 rounded-[var(--radius-md)] border border-border-soft bg-surface px-3 py-2.5 transition-colors hover:border-border-focus"
      style={{ boxShadow: "var(--elevation-raised)" }}
    >
      <span
        aria-hidden="true"
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold uppercase"
        style={{ background: "var(--primary-container)", color: "var(--on-primary-container)" }}
      >
        {church.churchName.slice(0, 1)}
      </span>
      <button onClick={onGo} className="flex min-w-0 flex-1 flex-col items-start text-left">
        <span className="truncate text-sm font-medium">{church.churchName}</span>
        <span className="truncate text-xs text-text-secondary">Código: {church.slug}</span>
      </button>
      <button
        onClick={onGo}
        className="shrink-0 rounded-full bg-primary-container px-3 py-1.5 text-xs font-medium text-on-primary-container transition-opacity hover:opacity-90"
      >
        Entrar
      </button>
      <button
        onClick={onForget}
        aria-label={`Remover ${church.churchName} da lista`}
        className="shrink-0 rounded-full p-1 text-text-muted opacity-0 transition-opacity hover:text-error group-hover:opacity-100 focus:opacity-100"
      >
        <X size={14} />
      </button>
    </div>
  );
}
