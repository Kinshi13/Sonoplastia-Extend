import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { getChurchBySlug } from "@/lib/church";
import { Announcement, RetrospectiveItem } from "@/lib/types/database";
import { announcementToRetrospectiveItem, RETROSPECTIVE_WINDOW_DAYS } from "@/lib/expiredAnnouncements";
import { EmptyState } from "@/components/EmptyState";
import { RetrospectivaGrid } from "@/components/RetrospectivaGrid";

export const revalidate = 0;

export default async function RetrospectivaPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const church = await getChurchBySlug(slug);
  if (!church || !church.is_active) notFound();

  const today = new Date();
  const todayStr = today.toISOString().slice(0, 10);
  const windowStart = new Date(today);
  windowStart.setDate(windowStart.getDate() - RETROSPECTIVE_WINDOW_DAYS);
  const windowStartStr = windowStart.toISOString().slice(0, 10);

  const supabase = await createClient();
  const [{ data: retrospectiveData }, { data: announcementData }] = await Promise.all([
    supabase
      .from("retrospective_items")
      .select("*")
      .eq("church_id", church.id)
      .eq("is_active", true),
    // Announcements whose event date has passed, but not more than RETROSPECTIVE_WINDOW_DAYS
    // ago - once older than that they fall out of both feeds entirely, with no cleanup needed.
    supabase
      .from("announcements")
      .select("*")
      .eq("church_id", church.id)
      .eq("is_active", true)
      .lt("related_event_date", todayStr)
      .gte("related_event_date", windowStartStr),
  ]);

  const nativeItems = (retrospectiveData as RetrospectiveItem[]) ?? [];
  const expiredAnnouncementItems = ((announcementData as Announcement[]) ?? []).map(
    announcementToRetrospectiveItem
  );

  const items = [...nativeItems, ...expiredAnnouncementItems].sort((a, b) => {
    const aTime = a.event_date ? new Date(a.event_date).getTime() : a.published_at;
    const bTime = b.event_date ? new Date(b.event_date).getTime() : b.published_at;
    return bTime - aTime;
  });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Retrospectiva</h2>
        <p className="mt-1 text-sm text-text-secondary">Fotos e vídeos dos últimos cultos e eventos.</p>
      </div>

      {items.length === 0 ? (
        <EmptyState message="Nenhuma foto ou vídeo publicado ainda." />
      ) : (
        <RetrospectivaGrid items={items} />
      )}
    </div>
  );
}
