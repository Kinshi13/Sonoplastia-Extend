import { notFound } from "next/navigation";
import { Pin, FileText } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Announcement, Bulletin } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";

export const revalidate = 0;

export default async function AnunciosPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const today = new Date().toISOString().slice(0, 10);
  const supabase = await createClient();
  const [{ data }, { data: bulletinsData }] = await Promise.all([
    supabase
      .from("announcements")
      .select("*")
      .eq("church_id", church.id)
      .eq("is_active", true)
      // An announcement leaves this feed once its event date has passed - it moves to the
      // Retrospectiva feed instead (see that page), it's never just deleted from here.
      .or(`related_event_date.is.null,related_event_date.gte.${today}`),
    supabase.from("bulletins").select("*").eq("church_id", church.id).eq("is_active", true),
  ]);

  const bulletinByAnnouncement = new Map<string, Bulletin>();
  for (const bulletin of (bulletinsData as Bulletin[]) ?? []) {
    if (bulletin.related_announcement_id) bulletinByAnnouncement.set(bulletin.related_announcement_id, bulletin);
  }

  const items = ((data as Announcement[]) ?? []).sort((a, b) => {
    if (a.is_pinned !== b.is_pinned) return a.is_pinned ? -1 : 1;
    return b.published_at - a.published_at;
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Anúncios</h2>
        <p className="mt-1 text-sm text-text-secondary">Novidades e avisos da igreja.</p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhum anúncio publicado ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          {items.map((announcement) => (
            <AnnouncementCard
              key={announcement.id}
              announcement={announcement}
              bulletin={bulletinByAnnouncement.get(announcement.id) ?? null}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function AnnouncementCard({
  announcement,
  bulletin,
}: {
  announcement: Announcement;
  bulletin: Bulletin | null;
}) {
  const hasMedia = !!announcement.media_url;

  return (
    <Card className="overflow-hidden flex flex-col hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      {hasMedia && announcement.media_type === "IMAGE" && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={announcement.media_url!}
          alt={announcement.title}
          className="aspect-4/3 w-full object-cover"
        />
      )}
      {hasMedia && announcement.media_type === "VIDEO" && (
        <video src={announcement.media_url!} controls className="aspect-4/3 w-full object-cover" />
      )}

      <div className="p-5 flex flex-col gap-2">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-semibold leading-tight">{announcement.title}</h3>
          {announcement.is_pinned && (
            <span className="flex items-center gap-1 shrink-0 text-xs font-medium text-primary">
              <Pin size={12} /> Fixado
            </span>
          )}
        </div>
        <p className="text-xs text-text-secondary">{formatPublishedAt(announcement.published_at)}</p>
        {announcement.description && (
          <p className="text-sm text-foreground/90 whitespace-pre-line">{announcement.description}</p>
        )}
        {bulletin && (
          <a
            href={bulletin.pdf_url}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-1 flex w-fit items-center gap-1.5 rounded-full bg-primary-container px-3 py-1.5 text-xs font-medium text-on-primary-container"
          >
            <FileText size={13} /> Ver boletim
          </a>
        )}
      </div>
    </Card>
  );
}
