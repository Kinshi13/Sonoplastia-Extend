import { notFound } from "next/navigation";
import { FileText } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Bulletin } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

export default async function BoletinsPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const supabase = await createClient();
  const { data } = await supabase
    .from("bulletins")
    .select("*")
    .eq("church_id", church.id)
    .eq("is_active", true)
    .order("published_at", { ascending: false });

  const items = (data as Bulletin[]) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Boletins</h2>
        <p className="mt-1 text-sm text-text-secondary">
          Informativos em PDF de departamentos e eventos da igreja.
        </p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhum boletim publicado ainda." />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {items.map((item) => (
            <a
              key={item.id}
              href={item.pdf_url}
              target="_blank"
              rel="noopener noreferrer"
              className="group block"
            >
              <Card className="overflow-hidden hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                <div className="relative aspect-4/3 w-full bg-background">
                  {item.cover_url ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={item.cover_url}
                      alt={item.title}
                      className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center text-text-secondary">
                      <FileText size={32} />
                    </div>
                  )}
                </div>
                <p className="p-3 text-sm font-medium leading-tight truncate">{item.title}</p>
              </Card>
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
