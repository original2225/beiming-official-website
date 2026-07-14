import { cn } from '../../utils/cn'

interface MarkdownPreviewProps {
  title: string
  body: string
  className?: string
}

export function MarkdownPreview({ title, body, className }: MarkdownPreviewProps) {
  const paragraphs = body.split(/\n{2,}/).filter(Boolean)
  return (
    <div
      className={cn(
        'panel-glass p-4 flex flex-col gap-4 h-full min-h-[16rem]',
        className
      )}
    >
      <h3 className="font-display text-lg text-indigo border-b border-rim pb-2">
        预览
      </h3>
      <div className="flex-1 overflow-auto">
        {title ? (
          <h4 className="font-display text-base text-text-primary mb-3">{title}</h4>
        ) : (
          <p className="text-text-muted text-sm italic">标题为空</p>
        )}
        {body ? (
          <div className="text-sm text-text-secondary leading-[1.7] space-y-3 whitespace-pre-wrap">
            {paragraphs.map((para, idx) => (
              <p key={idx}>{para}</p>
            ))}
          </div>
        ) : (
          <p className="text-text-muted text-sm italic">内容为空</p>
        )}
      </div>
      <p className="text-xs text-text-muted">
        预览仅做排版参考，发布后可查看完整效果。
      </p>
    </div>
  )
}
