import { Announcement, RetrospectiveItem } from "@/lib/types/database";

/** How long a concluded announcement keeps showing in the Retrospectiva feed. */
export const RETROSPECTIVE_WINDOW_DAYS = 20;

/** Used when a concluded announcement has no photo/video of its own to show as a card. */
export const ANNOUNCEMENT_FALLBACK_IMAGE = "/illustrations/announcement-fallback.jpg";

/** Adapts a concluded announcement into the shape RetrospectivaGrid already knows how to render. */
export function announcementToRetrospectiveItem(announcement: Announcement): RetrospectiveItem {
  const hasImage = announcement.media_type === "IMAGE" && !!announcement.media_url;
  const hasVideo = announcement.media_type === "VIDEO" && !!announcement.media_url;

  return {
    id: `announcement-${announcement.id}`,
    church_id: announcement.church_id,
    title: announcement.title,
    description: announcement.description,
    media_type: hasVideo ? "VIDEO" : "IMAGE",
    media_url: hasImage || hasVideo ? announcement.media_url! : ANNOUNCEMENT_FALLBACK_IMAGE,
    media_file_name: null,
    media_aspect_ratio: hasImage ? announcement.image_aspect_ratio : hasVideo ? "16:9" : "1:1",
    // Announcements never captured a video poster frame (unlike native Retrospectiva uploads) -
    // fall back to the illustration as the thumbnail while the real video still plays on open.
    poster_url: hasVideo ? ANNOUNCEMENT_FALLBACK_IMAGE : null,
    event_date: announcement.related_event_date,
    published_at: announcement.published_at,
    updated_at: announcement.updated_at,
    is_active: true,
  };
}
