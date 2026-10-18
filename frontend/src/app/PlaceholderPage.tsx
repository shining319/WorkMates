/** 功能还没做时，用它临时占位。做好真正的页面后，在 router.tsx 里替换掉。 */
export function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="mt-2 text-text-muted">Coming soon.</p>
    </div>
  )
}
