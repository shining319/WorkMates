import { Navigate, Outlet } from 'react-router'
import { useGetMeQuery } from '../features/auth/api'
import type { Role } from '../features/auth/types'
import { Button } from '../shared/ui/Button'
import { useAppSelector } from './store'

/**
 * Route guard (architecture §9.5). Without a token it redirects to /login.
 * With `allowed` set, any other role sees an access message. The backend checks
 * the role again on every request.
 */
export function RequireRole({ allowed }: { allowed?: Role }) {
  const token = useAppSelector((state) => state.auth.token)
  const user = useAppSelector((state) => state.auth.user)
  // After a page reload the token is back but the user is not: load it.
  const { isError, refetch } = useGetMeQuery(undefined, {
    skip: !token || !!user,
  })

  if (!token) {
    return <Navigate to="/login" replace />
  }
  if (!user) {
    // A 401 has already logged the user out above. This is a network or server failure.
    return isError ? (
      <div role="alert" className="p-4">
        <p>We could not load your account.</p>
        <Button className="mt-3" onClick={() => refetch()}>
          Try again
        </Button>
      </div>
    ) : (
      <p role="status" className="p-4 text-text-muted">
        Loading…
      </p>
    )
  }
  if (allowed && user.role !== allowed) {
    return (
      <p role="alert" className="p-4">
        You do not have access to this page
      </p>
    )
  }
  return <Outlet />
}
