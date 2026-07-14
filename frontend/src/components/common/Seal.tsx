/**
 * 印章装饰组件
 */

interface SealProps {
  text?: string
  className?: string
}

export function Seal({ text = '印', className = '' }: SealProps) {
  return (
    <span
      className={`
        inline-flex items-center justify-center
        w-8 h-8 rounded-sm
        bg-cinnabar text-rice-50
        font-display text-[0.8rem] leading-none
        shadow-sm select-none
        -rotate-6 border border-cinnabar/40
        ${className}
      `}
    >
      {text}
    </span>
  )
}
