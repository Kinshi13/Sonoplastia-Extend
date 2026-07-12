import { notFound } from "next/navigation";
import { getChurchBySlug } from "@/lib/church";
import { ChurchNav } from "@/components/ChurchNav";

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
      <div className="sticky top-0 z-10 -mx-5 flex flex-col gap-3 border-b border-divider bg-background/95 px-5 pb-4 pt-2 backdrop-blur-sm sm:static sm:mx-0 sm:flex-row sm:items-center sm:justify-between sm:px-0 sm:pt-0">
        <h1 className="text-lg font-semibold text-primary">{church.name}</h1>
        <ChurchNav links={navLinks} />
      </div>
      {children}
    </div>
  );
}
