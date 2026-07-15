import { redirect } from "next/navigation";
import { getAdminStatus } from "@/lib/supabase/auth";
import { getEntitlements } from "@/lib/entitlements";
import { AdminStellaCore } from "@/components/AdminStellaCore";
import { AdminNav } from "@/components/AdminNav";
import { signOutAction } from "./actions";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isAdmin, churchId } = await getAdminStatus();

  if (!user) redirect("/login");
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
    <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
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
      <AdminStellaCore hasAdvancedMedia={hasAdvancedMedia} />
    </div>
  );
}
