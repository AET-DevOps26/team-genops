import type { HTMLAttributes, ReactNode } from "react";

// The product's mono/uppercase status vernacular (`.tag` from index.css).
export function Tag({
  className = "",
  children,
  ...props
}: HTMLAttributes<HTMLSpanElement> & { children: ReactNode }) {
  return (
    <span className={`tag ${className}`} {...props}>
      {children}
    </span>
  );
}
