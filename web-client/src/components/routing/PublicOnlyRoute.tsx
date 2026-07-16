import { Navigate, Outlet } from "react-router-dom";
import { useAppSelector } from "~/store/hooks";
import { Splash } from "./Splash";

// Inverse gate: pages like /login that an authenticated user should never see.
// Same respect for `unknown` — wait for the bootstrap before redirecting either way.
export function PublicOnlyRoute() {
  const status = useAppSelector((s) => s.auth.status);
  if (status === "unknown") return <Splash />;
  if (status === "authenticated") return <Navigate to="/" replace />;
  return <Outlet />;
}
