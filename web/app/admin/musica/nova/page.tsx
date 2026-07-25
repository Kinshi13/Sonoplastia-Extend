import { MusicaForm } from "../MusicaForm";

export default function NovaMusicaPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Nova música</h1>
      <MusicaForm existing={null} />
    </div>
  );
}
