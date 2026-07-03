import { ScaleForm } from "../ScaleForm";

export default function NovaEscalaPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Nova escala</h1>
      <ScaleForm existing={null} />
    </div>
  );
}
