import type { InputHTMLAttributes, ReactNode } from "react";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  id: string;
  label: ReactNode;
  /** Optional affordance rendered inside the field, right-aligned (e.g. a show/hide toggle). */
  trailing?: ReactNode;
  /** Optional control rendered next to the label (e.g. a "Forgot?" link). */
  labelAside?: ReactNode;
}

// Wraps the semantic `.field` class so every input across services looks/focuses identically.
export function Field({
  id,
  label,
  trailing,
  labelAside,
  className = "",
  ...props
}: Props) {
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between">
        <label htmlFor={id} className="tag block text-dim">
          {label}
        </label>
        {labelAside}
      </div>
      <div className="field flex items-center gap-2 rounded-lg px-3.5 py-2.5">
        <input id={id} className={className} {...props} />
        {trailing}
      </div>
    </div>
  );
}
