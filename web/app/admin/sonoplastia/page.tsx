import { FileText, Image as ImageIcon, Video, File as FileIcon } from "lucide-react";
import { createClient } from "@/lib/supabase/server";
import { getAdminStatus } from "@/lib/supabase/auth";
import { SharedFile } from "@/lib/types/database";
import { Card } from "@/components/Card";
import { EmptyState } from "@/components/EmptyState";
import { DeleteButton } from "../DeleteButton";
import { deleteSharedFileAction } from "../actions";
import { UploadForm } from "./UploadForm";

export const revalidate = 0;

const MEDIA_ICONS: Record<SharedFile["media_type"], typeof FileText> = {
  IMAGE: ImageIcon,
  VIDEO: Video,
  DOCUMENT: FileText,
  NONE: FileIcon,
};

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default async function AdminSonoplastiaPage() {
  const { churchId } = await getAdminStatus();
  const supabase = await createClient();
  const { data } = await supabase
    .from("shared_files")
    .select("*")
    .eq("church_id", churchId)
    .order("uploaded_at", { ascending: false });
  const items = (data as SharedFile[]) ?? [];

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-2">Sonoplastia - arquivos remotos</h1>
      <p className="text-sm text-text-secondary mb-6">
        Envie apresentações, PDFs, fotos e vídeos aqui para acessar de qualquer dispositivo -
        inclusive pelo app no celular.
      </p>

      <UploadForm />

      <div className="mt-8">
        {items.length === 0 ? (
          <EmptyState message="Nenhum arquivo enviado ainda." />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {items.map((file) => {
              const Icon = MEDIA_ICONS[file.media_type] ?? FileIcon;
              return (
                <Card
                  key={file.id}
                  className="p-5 flex flex-col gap-3 hover:shadow-md hover:-translate-y-0.5"
                >
                  <div className="flex items-start gap-3">
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-container text-on-primary-container">
                      <Icon size={18} />
                    </span>
                    <div className="min-w-0">
                      <a
                        href={file.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="block font-medium text-primary truncate"
                      >
                        {file.file_name}
                      </a>
                      <p className="text-xs text-text-secondary">{formatSize(file.size_bytes)}</p>
                    </div>
                  </div>
                  <div className="mt-auto flex items-center justify-end border-t border-divider pt-3">
                    <DeleteButton id={file.id} action={deleteSharedFileAction} />
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
