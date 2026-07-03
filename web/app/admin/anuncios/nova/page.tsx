import { AnnouncementForm } from "../AnnouncementForm";

export default function NovoAnuncioPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-6">Novo anúncio</h1>
      <AnnouncementForm existing={null} />
    </div>
  );
}
