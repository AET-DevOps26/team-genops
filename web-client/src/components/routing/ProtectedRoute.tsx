import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '~/store/hooks'
import { Splash } from './Splash'

// Gate for authenticated-only routes. The four-state auth machine is the whole point:
//   unknown      → bootstrap (fetchMe) still in flight → show Splash, decide NOTHING
//   anonymous    → checked, no session                → redirect to /login
//   authenticated→ render the protected tree
// Collapsing `unknown` into `anonymous` would bounce a refreshing user to /login for a
// frame before the cookie check resolves — the flicker bug this state exists to prevent.
export function ProtectedRoute() {
  const status = useAppSelector((s) => s.auth.status)
  if (status === 'unknown') return <Splash />
  if (status !== 'authenticated') return <Navigate to="/login" replace />
  return <Outlet />
}
