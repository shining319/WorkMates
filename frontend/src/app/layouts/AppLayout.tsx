import { NavLink, Outlet } from 'react-router'
import { logout } from '../../features/auth/authSlice'
import { Button } from '../../shared/ui/Button'
import { useAppDispatch, useAppSelector } from '../store'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'font-semibold text-primary' : 'text-text-muted hover:text-text'

// Add an entry here with each feature. The notification bell goes here too.
export function AppLayout() {
  const role = useAppSelector((state) => state.auth.user?.role)
  const dispatch = useAppDispatch()

  return (
    <div className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <span className="font-bold text-primary">WorkMates</span>
          <nav aria-label="Main" className="flex flex-1 gap-4 text-sm">
            {role === 'STUDENT' && (
              <NavLink to="/jobs" className={navLinkClass}>
                Jobs
              </NavLink>
            )}
            {role === 'EMPLOYER' && (
              <NavLink to="/postings" className={navLinkClass}>
                Postings
              </NavLink>
            )}
          </nav>
          <Button variant="secondary" onClick={() => dispatch(logout())}>
            Log out
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
