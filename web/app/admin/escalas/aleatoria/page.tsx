import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { GenerateRandomScaleForm } from "./GenerateRandomScaleForm";

export default function GerarEscalaAleatoriaPage() {
  return (
    <div>
      <Link href="/admin/escalas" className="mb-4 flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-foreground">
        <ArrowLeft size={14} /> Voltar
      </Link>
      <h1 className="font-display text-2xl mb-1">Gerar escala aleatória</h1>
      <p className="text-sm text-text-secondary mb-6">
        Sorteia uma escala temporária para um dia específico - útil para uma escala de última hora,
        fora do ciclo oficial normal.
      </p>
      <GenerateRandomScaleForm />
    </div>
  );
}
