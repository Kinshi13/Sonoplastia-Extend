import { notFound } from "next/navigation";
import { getChurchBySlug } from "@/lib/church";
import { ChurchStellaDock } from "@/components/ChurchStellaDock";
import { RecentChurchRecorder } from "@/components/RecentChurchRecorder";

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

  // Fase 11.8 (Parte 2): the top now only carries the organization's identity - no repeated nav
  // links here anymore, that job belongs entirely to the Stella Dock at the bottom.
  return (
    <div className="flex flex-col gap-6 pb-28">
      <RecentChurchRecorder churchId={church.id} slug={slug} churchName={church.name} />
      <h1 className="font-display text-lg text-primary">{church.name}</h1>
      {children}
      <ChurchStellaDock slug={slug} />
    </div>
  );
}
