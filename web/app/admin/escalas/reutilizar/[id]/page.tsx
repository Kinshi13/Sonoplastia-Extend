import { notFound } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { Scale } from "@/lib/types/database";
import { ReuseScaleForm } from "./ReuseScaleForm";

export const revalidate = 0;

export default async function ReutilizarEscalaSourcePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const [{ data: source }, { data: assignments }] = await Promise.all([
    supabase.from("scales").select("*").eq("id", id).eq("church_id", churchId).single(),
    supabase.from("scale_assignments").select("role_name_snapshot").eq("scale_id", id).order("position", { ascending: true }),
  ]);

  if (!source) notFound();

  return (
    <div>
      <Link href="/admin/escalas/reutilizar" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Escolher outra escala
      </Link>
      <h1 className="font-display text-2xl mb-1">Reutilizar &quot;{(source as Scale).title}&quot;</h1>
      <p className="text-sm text-text-secondary mb-6">
        Isto cria uma <strong>nova</strong> escala com a mesma estrutura - a escala original não é
        alterada e as pessoas não são copiadas.
      </p>
      <ReuseScaleForm source={source as Scale} roleNames={(assignments ?? []).map((a) => a.role_name_snapshot)} />
    </div>
  );
}
