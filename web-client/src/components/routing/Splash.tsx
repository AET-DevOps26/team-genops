import { StatusDot } from "~/components/ui";

// Shown while the session bootstrap (fetchMe) is in flight — i.e. status === 'unknown'.
// Decides nothing; just holds the screen so neither gate redirects prematurely.
export function Splash() {
  return (
    <div className="grid min-h-screen place-items-center bg-ink">
      <StatusDot color="var(--color-offer)" live halo />
    </div>
  );
}
