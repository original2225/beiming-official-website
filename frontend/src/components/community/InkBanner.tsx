/**
 * 社区页顶部水墨横幅
 * 一眼可见的中式水墨头图：远山淡墨、书法标题、朱砂印章
 */

import type { ReactNode } from 'react'
import { Seal } from '../common/Seal'
import { InkStroke } from '../common/InkStroke'

interface InkBannerProps {
  title: string
  seal?: string
  subtitle?: ReactNode
  action?: ReactNode
}

export function InkBanner({ title, seal = '印', subtitle, action }: InkBannerProps) {
  return (
    <div className="relative overflow-hidden rounded-xl panel-ink p-8 mb-8 min-h-[10rem] flex flex-col justify-center">
      {/* 宣纸底纹 */}
      <svg
        className="absolute inset-0 w-full h-full opacity-[0.12] pointer-events-none"
        aria-hidden="true"
      >
        <filter id="banner-paper" x="0" y="0" width="100%" height="100%">
          <feTurbulence type="fractalNoise" baseFrequency="0.7" numOctaves="4" stitchTiles="stitch" />
        </filter>
        <rect width="100%" height="100%" filter="url(#banner-paper)" />
      </svg>

      {/* 水墨远山背景 */}
      <svg
        viewBox="0 0 800 200"
        preserveAspectRatio="xMidYMid slice"
        className="absolute inset-0 w-full h-full text-ink-600 pointer-events-none"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="mountain-fade" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.35" />
            <stop offset="55%" stopColor="currentColor" stopOpacity="0.15" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path
          d="M0,200 C120,110 220,130 320,170 C420,210 520,90 620,110 C720,130 760,70 800,90 L800,200 Z"
          fill="url(#mountain-fade)"
        />
        <path
          d="M0,200 C150,150 250,180 400,160 C550,140 650,190 800,170 L800,200 Z"
          fill="currentColor"
          opacity="0.22"
        />
        <path
          d="M0,200 C100,185 200,195 300,188 C400,180 500,198 600,192 C700,185 760,195 800,190 L800,200 Z"
          fill="currentColor"
          opacity="0.12"
        />
      </svg>

      {/* 内容 */}
      <div className="relative z-10 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-4 mb-3">
            <h1 className="font-calligraphy text-5xl text-ink-900 drop-shadow-sm tracking-[0.15em]">{title}</h1>
            <Seal text={seal} />
          </div>
          {subtitle && <p className="text-base text-text-secondary">{subtitle}</p>}
        </div>
        {action && <div className="self-start">{action}</div>}
      </div>

      <InkStroke className="relative z-10 mt-6" color="text-ink-600/50" />
    </div>
  )
}
