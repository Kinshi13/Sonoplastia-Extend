export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-divider p-10 text-center text-text-secondary">
      {message}
    </div>
  );
}
