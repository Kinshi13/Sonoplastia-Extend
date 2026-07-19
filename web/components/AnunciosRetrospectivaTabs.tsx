import Link from "next/link";

/**
 * Retrospectiva pré-lançamento correction: a visible selector between the two related public
 * feeds. Each tab is a real nested route (`/c/[slug]/anuncios`, `/c/[slug]/retrospectiva`), not
 * client-side tab state - so the active tab is always reflected in the URL, reloads keep working,
 * and the retrospectiva can be opened/shared as a direct link, all for free from Next.js routing.
 */
export function AnunciosRetrospectivaTabs({
  slug,
  active,
}: {
  slug: string;
  active: "anuncios" | "retrospectiva";
}) {
  const tabs = [
    { key: "anuncios" as const, label: "Anúncios", href: `/c/${slug}/anuncios` },
    { key: "retrospectiva" as const, label: "Retrospectiva", href: `/c/${slug}/retrospectiva` },
  ];

  return (
    <div
      role="tablist"
      aria-label="Alternar entre Anúncios e Retrospectiva"
      className="inline-flex w-fit rounded-full border border-divider bg-surface p-1 text-sm"
    >
      {tabs.map((tab) => {
        const isActive = tab.key === active;
        return (
          <Link
            key={tab.key}
            href={tab.href}
            role="tab"
            aria-current={isActive ? "page" : undefined}
            className={`rounded-full px-4 py-1.5 font-medium transition-colors ${
              isActive ? "bg-primary text-white" : "text-text-secondary hover:text-foreground"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </div>
  );
}
