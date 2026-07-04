import { RetrospectivaForm } from "../RetrospectivaForm";

export default function NovaRetrospectivaPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Nova publicação</h1>
      <RetrospectivaForm existing={null} />
    </div>
  );
}
