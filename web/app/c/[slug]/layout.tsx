import { notFound } from "next/navigation";
import Link from "next/link";
import { getChurchBySlug } from "@/lib/church";

export default async function ChurchLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);

  if (!church) notFound();

  if (!church.is_active) {
    return (
      <div className="mx-auto max-w-md text-center">
        <h1 className="text-xl font-semibold mb-2">{church.name}</h1>
        <p className="text-sm text-text-secondary">
          O cadastro dessa igreja ainda está sendo ativado. Se você é o administrador e acabou de
          concluir o pagamento, aguarde alguns instantes e recarregue a página.
        </p>
      </div>
    );
  }

  const navLinks = [
    { href: `/c/${slug}`, label: "Escala" },
    { href: `/c/${slug}/doxologia`, label: "Doxologia" },
    { href: `/c/${slug}/anuncios`, label: "Anúncios" },
    { href: `/c/${slug}/boletins`, label: "Boletins" },
    { href: `/c/${slug}/retrospectiva`, label: "Retrospectiva" },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-divider pb-4">
        <h1 className="text-lg font-semibold text-primary">{church.name}</h1>
        <nav className="flex flex-wrap gap-4 text-sm">
          {navLinks.map((link) => (
            <Link key={link.href} href={link.href} className="text-foreground/80 hover:text-primary transition-colors">
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
      {children}
    </div>
  );
}
