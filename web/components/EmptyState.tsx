export function EmptyState({ message }: { message: string }) {
  return (
    <div className="relative overflow-hidden rounded-[var(--radius-lg)] border border-dashed border-border-soft bg-surface/40 p-12 text-center">
      <span aria-hidden="true" className="pointer-events-none absolute -top-6 right-8 text-3xl text-accent-star/30">
        ✦
      </span>
      <span aria-hidden="true" className="pointer-events-none absolute bottom-4 left-10 text-lg text-accent-constellation/20">
        ✦
      </span>
      <p className="relative text-text-secondary">{message}</p>
    </div>
  );
}
