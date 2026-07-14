interface SkeletonCardProps {
  variant: 'board' | 'post'
  count?: number
}

export function SkeletonCard({ variant, count = 1 }: SkeletonCardProps) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) =>
        variant === 'board' ? (
          <div
            key={i}
            className="flex flex-col gap-3 p-4 panel-glass border-l-4 border-l-transparent animate-pulse"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-surface-light" />
              <div className="h-4 w-24 bg-surface-light" />
            </div>
            <div className="h-3 w-full bg-surface-light" />
            <div className="h-3 w-2/3 bg-surface-light" />
            <div className="mt-auto pt-2 border-t border-rim flex justify-between">
              <div className="h-3 w-16 bg-surface-light" />
              <div className="h-3 w-12 bg-surface-light" />
            </div>
          </div>
        ) : (
          <div
            key={i}
            className="flex items-start gap-3 p-3 bg-surface-elevated border border-rim animate-pulse"
          >
            <div className="w-10 h-10 bg-surface-light shrink-0" />
            <div className="flex-1 min-w-0 flex flex-col gap-2">
              <div className="h-4 w-full bg-surface-light" />
              <div className="h-3 w-1/2 bg-surface-light" />
            </div>
          </div>
        )
      )}
    </>
  )
}
