import { Link, Outlet } from 'react-router'

export function PublicLayout() {
  return (
    <div className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <Link to="/" className="flex-1 font-bold text-primary">
            WorkMates
          </Link>
          <nav aria-label="Account" className="flex gap-4 text-sm">
            <Link to="/login">Log in</Link>
            <Link to="/register">Register</Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
