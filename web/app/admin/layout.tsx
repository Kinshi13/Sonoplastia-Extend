import { redirect } from "next/navigation";
import { cookies } from "next/headers";
import { getAdminStatus } from "@/lib/supabase/auth";
import { getEntitlements } from "@/lib/entitlements";
import { AdminStellaCore } from "@/components/AdminStellaCore";
import { AdminStellaDock } from "@/components/AdminStellaDock";
import { AdminNav } from "@/components/AdminNav";
import { signOutAction } from "./actions";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isAdmin, churchId } = await getAdminStatus();

  if (!user) {
    // Bloco E: distinguishes "never logged in" from "had a session that expired" using the real
    // Supabase auth cookie, not a guess - only redirect with ?expired=1 when a (now-invalid)
    // session cookie was actually present, so the login page's warning is never shown to someone
    // who simply never signed in.
    const cookieStore = await cookies();
    const hadSessionCookie = cookieStore.getAll().some((cookie) => cookie.name.startsWith("sb-"));
    redirect(hadSessionCookie ? "/login?expired=1" : "/login");
  }
  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-md text-center">
        <h1 className="text-xl font-semibold mb-2">Sem acesso de administrador</h1>
        <p className="text-sm text-text-secondary">
          Sua conta está autenticada, mas não administra nenhuma igreja ainda. Se você acabou de
          comprar, aguarde a confirmação do pagamento - senão, fale com quem gerencia o projeto.
        </p>
      </div>
    );
  }

  const entitlements = churchId ? await getEntitlements(churchId) : null;
  const hasAdvancedMedia = entitlements?.features.has("ADVANCED_MEDIA") ?? false;

  return (
    <div className="flex flex-col gap-6 pb-28 lg:flex-row lg:items-start lg:gap-8 lg:pb-6">
      <AdminNav planName={entitlements?.planName} />

      <div className="flex min-w-0 flex-1 flex-col gap-6">
        <div className="flex items-center justify-between gap-3 border-b border-border-soft pb-4">
          <p className="text-sm font-medium text-text-secondary lg:hidden">Escala Church · Admin</p>
          <form action={signOutAction} className="ml-auto">
            <button
              type="submit"
              className="rounded-full border border-border-soft px-3 py-1 text-xs text-text-secondary hover:text-foreground transition-colors"
            >
              Sair
            </button>
          </form>
        </div>
        {children}
      </div>

      {/* Fase 11.8 (Parte 3): floating núcleo on desktop (sidebar already carries the nav job
          there), full dock replacing both the pill row and the lone star on mobile. */}
      <div className="hidden lg:block">
        <AdminStellaCore hasAdvancedMedia={hasAdvancedMedia} />
      </div>
      <div className="lg:hidden">
        <AdminStellaDock hasAdvancedMedia={hasAdvancedMedia} />
      </div>
    </div>
  );
}
