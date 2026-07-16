"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Eye, ShieldCheck } from "lucide-react";

/**
 * Hotfix (Parte 11-15): a quick toggle between the public site and the Admin panel for a
 * signed-in admin - never shown to public visitors or plain members (only rendered by the root
 * layout when `getAdminStatus()` already confirmed `isAdmin`, server-side). This component only
 * navigates; it grants no permission of its own - every /admin route stays protected by
 * getAdminStatus()/RLS regardless of whether this button exists (Parte 15).
 */
export function AdminViewSwitcher({ churchSlug }: { churchSlug: string | null }) {
  const pathname = usePathname();
  const inAdmin = pathname.startsWith("/admin");

  if (inAdmin) {
    return (
      <Link
        href={churchSlug ? `/c/${churchSlug}` : "/"}
        className="flex items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-sm font-medium text-text-secondary hover:text-foreground hover:bg-surface transition-colors"
      >
        <Eye size={14} /> Ver público
      </Link>
    );
  }

  return (
    <Link
      href="/admin"
      className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-1.5 text-sm font-medium text-white shadow-[var(--elevation-raised)] hover:opacity-90 transition-opacity"
    >
      <ShieldCheck size={14} /> Modo Admin
    </Link>
  );
}
