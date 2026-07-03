import { DoxologyForm } from "../DoxologyForm";

export default function NovaDoxologiaPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Nova doxologia</h1>
      <DoxologyForm existing={null} />
    </div>
  );
}
