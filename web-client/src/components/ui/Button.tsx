import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "ghost";

// Hand-rolled variant map (no cva): plain object, ~5 primitives don't need a DSL.
// `primary` reuses the semantic `.cta` class from index.css.
const VARIANTS: Record<Variant, string> = {
  primary: "cta",
  ghost: "bg-raised-2 text-fg border border-line hover:brightness-110",
};

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
  children: ReactNode;
}

export function Button({
  variant = "primary",
  loading = false,
  className = "",
  disabled,
  children,
  ...props
}: Props) {
  return (
    <button
      className={`rounded-lg py-3 text-[15px] font-medium transition ${VARIANTS[variant]} ${className}`}
      disabled={loading || disabled}
      aria-busy={loading}
      {...props}
    >
      {children}
    </button>
  );
}
