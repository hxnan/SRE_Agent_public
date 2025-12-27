import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const toNumber = (v: unknown): number => {
  if (typeof v === 'number') return v
  if (typeof v === 'string') {
    const n = parseFloat(v)
    return Number.isFinite(n) ? n : NaN
  }
  return NaN
}

export const formatPrice = (price: unknown): string => {
  const n = toNumber(price)
  return Number.isFinite(n) ? `¥${n.toFixed(2)}` : '¥-'
}
