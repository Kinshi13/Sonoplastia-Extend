import { redirect } from "next/navigation";
import Link from "next/link";
import { getAdminStatus } from "@/lib/supabase/auth";
import { signOutAction } from "./actions";

const adminLinks = [
  { href: "/admin", label: "Painel" },
  { href: "/admin/escalas", label: "Escalas" },
  { href: "/admin/doxologia", label: "Doxologia" },
  { href: "/admin/anuncios", label: "Anúncios" },
  { href: "/admin/boletins", label: "Boletins" },
  { href: "/admin/retrospectiva", label: "Retrospectiva" },
  { href: "/admin/sonoplastia", label: "Sonoplastia" },
  { href: "/admin/planos", label: "Planos" },
];

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isAdmin } = await getAdminStatus();

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

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-divider pb-4">
        <nav className="flex flex-wrap gap-4 text-sm">
          {adminLinks.map((link) => (
            <Link key={link.href} href={link.href} className="text-foreground/80 hover:text-primary">
              {link.label}
            </Link>
          ))}
        </nav>
        <form action={signOutAction}>
          <button
            type="submit"
            className="rounded-full border border-divider px-3 py-1 text-xs text-text-secondary hover:text-foreground"
          >
            Sair
          </button>
        </form>
      </div>
      {children}
    </div>
  );
}
