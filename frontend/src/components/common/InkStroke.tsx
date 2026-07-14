/**
 * 墨痕分隔线
 */

interface InkStrokeProps {
  className?: string
  color?: string
}

export function InkStroke({ className = '', color = 'text-ink-600/60' }: InkStrokeProps) {
  return (
    <svg
      viewBox="0 0 1200 24"
      preserveAspectRatio="none"
      className={`w-full h-8 ${color} ${className}`}
      aria-hidden="true"
    >
      <path
        d="M0,14 C150,10 250,18 400,12 C550,6 650,20 800,13 C950,6 1050,16 1200,12 L1200,16 C1050,20 950,10 800,17 C650,24 550,10 400,16 C250,22 150,14 0,18 Z"
        fill="currentColor"
      />
    </svg>
  )
}
