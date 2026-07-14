import { cn } from '../../utils/cn'

interface TagChipProps {
  label: string
  active?: boolean
  onClick?: () => void
}

export function TagChip({ label, active, onClick }: TagChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn('chip-ink', active && 'chip-ink-active')}
    >
      {label}
    </button>
  )
}
