/** Stands in for a page until its feature is built. */
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="mt-2 text-text-muted">Coming soon.</p>
    </div>
  )
}
