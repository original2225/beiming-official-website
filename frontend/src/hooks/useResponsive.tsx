/**
 * 响应式断点 hook — 判断当前屏幕尺寸
 * 供需要响应式布局的组件使用（后续阶段启用）
 */

import { useState, useEffect } from 'react'

type Breakpoint = 'sm' | 'md' | 'lg' | 'xl'

export function useResponsive(): Breakpoint {
  const [bp, setBp] = useState<Breakpoint>('lg')

  useEffect(() => {
    const check = () => {
      const w = window.innerWidth
      if (w < 640) setBp('sm')
      else if (w < 768) setBp('md')
      else if (w < 1024) setBp('lg')
      else setBp('xl')
    }
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  return bp
}
