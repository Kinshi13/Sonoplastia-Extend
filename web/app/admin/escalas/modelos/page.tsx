import Link from "next/link";
import { ArrowLeft, Plus, ShieldCheck, RotateCcw } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { ScaleTemplate } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../../DeleteButton";
import { ensureDefaultScaleTemplatesAction, deleteScaleTemplateAction } from "../../actions";

export const revalidate = 0;

/** "Escalas padrão" - modelos reutilizáveis independentes de qualquer escala real já ter sido
 *  excluída (o problema que "Reutilizar" tinha: se a escala de origem sumia, não sobrava nada pra
 *  reaproveitar). Os 3 modelos fixos (quarta/sábado/domingo) são semeados automaticamente aqui. */
export default async function ScaleTemplatesPage() {
  const { churchId } = await getAdminStatus();
  await ensureDefaultScaleTemplatesAction(churchId!);
  const supabase = await createClient();
  const { data } = await supabase
    .from("scale_templates")
    .select("*")
    .eq("church_id", churchId)
    .order("is_protected", { ascending: false })
    .order("name", { ascending: true });
  const items = (data as ScaleTemplate[]) ?? [];

  return (
    <div>
      <Link href="/admin/escalas" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Voltar
      </Link>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
        <h1 className="font-display text-2xl">Escalas padrão</h1>
        <Link
          href="/admin/escalas/modelos/nova"
          className="flex items-center gap-1.5 rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
        >
          <Plus size={16} /> Novo modelo
        </Link>
      </div>
      <p className="text-sm text-text-secondary mb-6">
        Modelos guardam só a estrutura (funções e ordem) - sempre disponíveis para reutilizar, mesmo
        que a última escala real daquele dia já tenha sido excluída. Os 3 marcados como padrão não
        podem ser excluídos, só editados.
      </p>

      {items.length === 0 ? (
        <EmptyState message="Nenhum modelo cadastrado ainda." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map((template) => (
            <Card key={template.id} className="p-5 flex flex-col gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-semibold leading-tight">{template.name}</p>
                  {template.is_protected && (
                    <span className="flex shrink-0 items-center gap-1 rounded-full bg-primary-container px-2 py-0.5 text-xs font-medium text-on-primary-container">
                      <ShieldCheck size={11} /> Padrão
                    </span>
                  )}
                </div>
                <p className="mt-1 text-sm text-text-secondary">
                  {template.roles.length} {template.roles.length === 1 ? "função" : "funções"}
                </p>
                {template.description && <p className="mt-1 text-xs text-text-secondary">{template.description}</p>}
              </div>
              <div className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-2 border-t border-divider pt-3">
                <Link
                  href={`/admin/escalas/modelos/${template.id}/aplicar`}
                  className="flex items-center gap-1.5 text-sm font-medium text-primary"
                >
                  <RotateCcw size={13} /> Usar este
                </Link>
                <Link href={`/admin/escalas/modelos/${template.id}`} className="text-sm font-medium text-primary">
                  Editar
                </Link>
                {!template.is_protected && <DeleteButton id={template.id} action={deleteScaleTemplateAction} />}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
