import Link from "next/link";
import { RotateCcw } from "lucide-react";
import { DoxologyForm } from "../DoxologyForm";

export default function NovaDoxologiaPage() {
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Nova doxologia</h1>
        <Link
          href="/admin/doxologia/reutilizar"
          className="flex items-center gap-1.5 rounded-full border border-border-soft px-4 py-2 text-sm font-medium text-primary hover:bg-primary-container/20 transition-colors"
        >
          <RotateCcw size={14} /> Reutilizar programação recente
        </Link>
      </div>
      <DoxologyForm existing={null} />
    </div>
  );
}
