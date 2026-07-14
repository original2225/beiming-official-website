import { cn } from '../../utils/cn'

interface AvatarProps {
  name: string
  url?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeMap = {
  sm: 'w-6 h-6 text-[0.6rem]',
  md: 'w-10 h-10 text-sm',
  lg: 'w-14 h-14 text-base',
}

export function Avatar({ name, url, size = 'md', className }: AvatarProps) {
  const initial = name.charAt(0).toUpperCase()
  return (
    <div
      className={cn(
        'flex items-center justify-center shrink-0 overflow-hidden rounded-full border-2 border-ink-600/40 bg-surface-dark text-indigo font-display',
        sizeMap[size],
        className
      )}
    >
      {url ? (
        <img src={url} alt={name} className="w-full h-full object-cover pixel-perfect" />
      ) : (
        <span>{initial}</span>
      )}
    </div>
  )
}
