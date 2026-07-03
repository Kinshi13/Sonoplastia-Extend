import { createClient } from "@/lib/supabase/server";
import { Announcement } from "@/lib/types/database";
import { formatPublishedAt } from "@/lib/format";

export const revalidate = 0;

export default async function AnunciosPage() {
  const supabase = await createClient();
  const { data } = await supabase
    .from("announcements")
    .select("*")
    .eq("is_active", true);

  const items = ((data as Announcement[]) ?? []).sort((a, b) => {
    if (a.is_pinned !== b.is_pinned) return a.is_pinned ? -1 : 1;
    return b.published_at - a.published_at;
  });

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold">Anúncios</h1>

      {items.length === 0 ? (
        <p className="text-text-secondary">Nenhum anúncio publicado ainda.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          {items.map((announcement) => (
            <AnnouncementCard key={announcement.id} announcement={announcement} />
          ))}
        </div>
      )}
    </div>
  );
}

function AnnouncementCard({ announcement }: { announcement: Announcement }) {
  const hasMedia = !!announcement.media_url;

  return (
    <div className="overflow-hidden rounded-2xl bg-surface shadow-sm border border-divider flex flex-col">
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
          <h2 className="font-semibold">{announcement.title}</h2>
          {announcement.is_pinned && (
            <span className="text-xs text-primary shrink-0">Fixado</span>
          )}
        </div>
        <p className="text-xs text-text-secondary">{formatPublishedAt(announcement.published_at)}</p>
        {announcement.description && (
          <p className="text-sm">{announcement.description}</p>
        )}
      </div>
    </div>
  );
}
