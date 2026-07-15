import { notFound } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Doxology } from "@/lib/types/database";
import { ReuseForm } from "./ReuseForm";

export const revalidate = 0;

export default async function ReutilizarDoxologiaSourcePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase.from("doxologies").select("*").eq("id", id).eq("church_id", churchId).single();

  if (!data) notFound();
  const source = data as Doxology;

  return (
    <div>
      <Link
        href="/admin/doxologia/reutilizar"
        className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground"
      >
        <ArrowLeft size={14} /> Escolher outra programação
      </Link>
      <h1 className="font-display text-2xl mb-1">Reutilizar &quot;{source.title}&quot;</h1>
      <p className="text-sm text-text-secondary mb-6">
        Isto cria uma <strong>nova</strong> Doxologia a partir desta - a programação original não é
        alterada.
      </p>
      <ReuseForm source={source} />
    </div>
  );
}
