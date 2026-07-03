import { createClient } from "@/lib/supabase/server";
import { SharedFile } from "@/lib/types/database";
import { DeleteButton } from "../DeleteButton";
import { deleteSharedFileAction } from "../actions";
import { UploadForm } from "./UploadForm";

export const revalidate = 0;

export default async function AdminSonoplastiaPage() {
  const supabase = await createClient();
  const { data } = await supabase
    .from("shared_files")
    .select("*")
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

      <div className="mt-8 flex flex-col gap-3">
        {items.map((file) => (
          <div
            key={file.id}
            className="flex items-center justify-between gap-3 rounded-xl bg-surface p-4 border border-divider"
          >
            <a
              href={file.url}
              target="_blank"
              rel="noopener noreferrer"
              className="font-medium text-primary truncate"
            >
              {file.file_name}
            </a>
            <DeleteButton id={file.id} action={deleteSharedFileAction} />
          </div>
        ))}
        {items.length === 0 && <p className="text-text-secondary">Nenhum arquivo enviado ainda.</p>}
      </div>
    </div>
  );
}
