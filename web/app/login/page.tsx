"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Eye, EyeOff, ShieldCheck, ArrowLeft } from "lucide-react";
import { createClient } from "@/lib/supabase/client";

/**
 * Fase 11.8.2 (Bloco E): "AdminLoginCard" - the admin area gets its own visual identity (a sober
 * Celestial Frame, an explicit "Área administrativa" seal) instead of reusing the same generic
 * card as the public code entry, so the two access paths never look interchangeable (Bloco E1).
 * Password recovery calls real Supabase Auth (`resetPasswordForEmail`) - no mock flow.
 */
export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginCard />
    </Suspense>
  );
}

function LoginCard() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const expired = searchParams.get("expired") === "1";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [resetSent, setResetSent] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    const supabase = createClient();
    const { error: signInError } = await supabase.auth.signInWithPassword({ email, password });

    setLoading(false);
    if (signInError) {
      setError("Não foi possível entrar. Confira o e-mail e a senha.");
      return;
    }

    router.push("/admin");
    router.refresh();
  }

  async function handleResetPassword() {
    if (!email) {
      setError("Informe seu e-mail acima para receber o link de recuperação.");
      return;
    }
    setResetLoading(true);
    setError(null);
    const supabase = createClient();
    const { error: resetError } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: typeof window !== "undefined" ? `${window.location.origin}/login` : undefined,
    });
    setResetLoading(false);
    if (resetError) {
      setError("Não foi possível enviar o e-mail de recuperação. Tente novamente.");
      return;
    }
    setResetSent(true);
  }

  return (
    <div className="mx-auto max-w-sm py-8 sm:py-16">
      <div
        className="relative overflow-hidden rounded-[var(--radius-hero)] border p-8"
        style={{
          borderColor: "var(--border-soft)",
          background: `linear-gradient(160deg, var(--surface) 0%, var(--surface-elevated) 100%)`,
          boxShadow: "var(--elevation-floating)",
        }}
      >
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-8 -top-8 h-40 w-40 rounded-full opacity-[0.08]"
          style={{ background: "radial-gradient(circle, var(--cc-polaris) 0%, transparent 70%)" }}
        />

        <div className="relative mb-6 flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-[0.14em] text-accent-constellation">
            <span aria-hidden="true">✦</span> Escala Church
          </div>
          <span
            className="flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide"
            style={{ background: "var(--primary-container)", color: "var(--on-primary-container)" }}
          >
            <ShieldCheck size={11} /> Admin
          </span>
        </div>

        <h1 className="font-display text-2xl mb-1 relative">Área administrativa</h1>
        <p className="text-sm text-text-secondary mb-6 relative">
          Entre com a conta de administrador para editar escalas, doxologia e anúncios.
        </p>

        {expired && (
          <p className="mb-4 rounded-[var(--radius-sm)] border border-warning/30 bg-warning/10 px-3 py-2 text-sm text-warning relative">
            Sua sessão expirou. Entre novamente para continuar.
          </p>
        )}

        <form onSubmit={handleSubmit} className="relative flex flex-col gap-4">
          <div>
            <label className="text-sm font-medium block mb-1" htmlFor="email">
              E-mail
            </label>
            <input
              id="email"
              type="email"
              required
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-[var(--radius-sm)] border border-border-soft bg-background px-3 py-2 text-sm outline-none focus:border-border-focus transition-colors"
            />
          </div>
          <div>
            <div className="mb-1 flex items-center justify-between">
              <label className="text-sm font-medium" htmlFor="password">
                Senha
              </label>
              <button
                type="button"
                onClick={handleResetPassword}
                disabled={resetLoading}
                className="text-xs font-medium text-primary hover:underline disabled:opacity-60"
              >
                {resetLoading ? "Enviando..." : "Esqueci minha senha"}
              </button>
            </div>
            <div className="relative">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-[var(--radius-sm)] border border-border-soft bg-background px-3 py-2 pr-10 text-sm outline-none focus:border-border-focus transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-text-muted hover:text-foreground"
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {resetSent && (
            <p className="text-sm text-success">
              Enviamos um link de recuperação para {email}. Verifique sua caixa de entrada.
            </p>
          )}
          {error && <p className="text-sm text-error">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white [box-shadow:var(--elevation-raised)] hover:opacity-90 transition-opacity disabled:opacity-60"
          >
            {loading ? "Entrando..." : "Entrar"}
          </button>
        </form>

        <Link
          href="/"
          className="relative mt-6 flex items-center justify-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground"
        >
          <ArrowLeft size={14} /> Voltar à área pública
        </Link>
      </div>
    </div>
  );
}
