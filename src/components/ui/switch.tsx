"use client"

import * as React from "react"
import * as SwitchPrimitive from "@radix-ui/react-switch"

import { cn } from "@/lib/utils"

/**
 * Theme-aware switch — explicit CSS vars (never undefined), RTL-aware thumb travel.
 * 44x24px track = comfortable touch target, accent gradient when checked.
 */
function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "peer inline-flex h-6 w-11 shrink-0 items-center rounded-full border p-0 transition-all outline-none",
        "border-[var(--border)] bg-[var(--surface-2)]",
        "data-[state=checked]:border-transparent data-[state=checked]:bg-[var(--accent)] data-[state=checked]:shadow-[var(--glow-soft)]",
        "focus-visible:ring-[3px] focus-visible:ring-[color-mix(in_srgb,var(--accent)_35%,transparent)]",
        "disabled:cursor-not-allowed disabled:opacity-50",
        className
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className={cn(
          "pointer-events-none block size-[18px] rounded-full bg-white shadow-[0_1px_4px_rgba(0,0,0,0.45)] transition-transform duration-200",
          "translate-x-[2px]",
          "data-[state=checked]:translate-x-[22px]",
          "rtl:translate-x-[-2px] rtl:data-[state=checked]:translate-x-[-22px]"
        )}
      />
    </SwitchPrimitive.Root>
  )
}

export { Switch }
