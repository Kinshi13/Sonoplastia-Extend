import { notFound } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { ScaleTemplate } from "@/lib/types/database";
import { ApplyScaleTemplateForm } from "./ApplyScaleTemplateForm";

export const revalidate = 0;

export default async function AplicarModeloPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data: template } = await supabase.from("scale_templates").select("*").eq("id", id).eq("church_id", churchId).single();
  if (!template) notFound();

  const roleIds = ((template as ScaleTemplate).roles ?? []).map((r) => r.roleId);
  const { data: roles } = roleIds.length
    ? await supabase.from("organization_roles").select("id, name").in("id", roleIds)
    : { data: [] as { id: string; name: string }[] };
  const roleNameById = new Map((roles ?? []).map((r) => [r.id, r.name]));
  const roleNames = [...(template as ScaleTemplate).roles]
    .sort((a, b) => a.position - b.position)
    .map((r) => roleNameById.get(r.roleId))
    .filter((name): name is string => !!name);

  return (
    <div>
      <Link href="/admin/escalas/modelos" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Escolher outro modelo
      </Link>
      <h1 className="font-display text-2xl mb-1">Usar &quot;{(template as ScaleTemplate).name}&quot;</h1>
      <p className="text-sm text-text-secondary mb-6">
        Isto cria uma <strong>nova</strong> escala com a estrutura deste modelo - o modelo em si não é alterado.
      </p>
      <ApplyScaleTemplateForm template={template as ScaleTemplate} roleNames={roleNames} />
    </div>
  );
}
