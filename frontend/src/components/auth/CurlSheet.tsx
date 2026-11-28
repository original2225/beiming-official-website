/**
 * 一张会「弯」的活页纸（花蝉 2026-11-28：要物理翻动，不是平移）
 *
 * 为什么单层 rotateX 不行：
 *   单层纸是一块硬板，绕上边框转到 90° 时正好侧对视线 —— 只剩一条线，
 *   然后突然消失/出现。观感就是「换了一张图」，不像纸。
 *
 * 真实纸张翻起来是【弯的】：靠近折痕处还没怎么动，远端已经甩过去了。
 * 做法是把纸拆成 N 层嵌套，每层相对父层多转一点（bend），累加成弧面 ——
 * 即 "CSS 3D Bending Effect" 的思路：the more pieces you have, the smoother the bend is.
 *
 * 结构要点（踩过坑）：
 *   · 最外层必须是 absolute inset:0，高度由外层 .bm-page 决定。
 *     写成 position:relative 而内容又是 absolute，整张纸会塌成 0 高，什么也看不到。
 *   · 只有最外层画内容，内层只画纸面 + 折痕阴影，否则内容被复印 N 遍。
 */

import type { CSSProperties, ReactNode } from 'react'

/** 纸被切成几段。6 段在这个尺寸上已看不出台阶。 */
export const CURL_SEGMENTS = 6

interface CurlSheetProps {
  children?: ReactNode
  /** 整张纸的角度：0 = 平放，-180 = 完全翻上去 */
  angle?: number
  /** 弯曲量：每内层多转多少度。0 = 硬板；6~9 比较像纸 */
  bend?: number
  paperColor?: string
  ruleColor?: string
  className?: string
}

export function CurlSheet({
  children,
  angle = 0,
  bend = 7,
  paperColor = '#FCFCF8',
  ruleColor = 'rgba(46, 90, 107, 0.09)',
  className = '',
}: CurlSheetProps) {
  let node: ReactNode = null
  for (let i = CURL_SEGMENTS - 1; i >= 0; i--) {
    const isOuter = i === 0
    const depth = i

    const paper: CSSProperties = {
      position: 'absolute',
      inset: 0,
      borderRadius: isOuter ? '6px' : '3px',
      background: paperColor,
      backgroundImage: `repeating-linear-gradient(180deg, transparent 0 27px, ${ruleColor} 27px 28px)`,
      // 每层底部压一道暗影 = 折痕明暗；层层累加，弧面的暗部就出来了
      boxShadow: isOuter
        ? '0 10px 30px rgba(0,0,0,0.26)'
        : 'inset 0 -6px 9px -6px rgba(0,0,0,0.34)',
    }

    node = (
      <div
        style={{
          // 关键：绝对定位铺满父级，高度才不是 0
          position: 'absolute',
          inset: 0,
          transformOrigin: 'top center',
          transformStyle: 'preserve-3d',
          transform: isOuter ? `rotateX(${angle}deg)` : `rotateX(${-bend}deg)`,
          zIndex: isOuter ? 1 : CURL_SEGMENTS - depth,
        }}
      >
        <div style={paper} />
        {isOuter && (
          <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
            {children}
          </div>
        )}
        {node}
      </div>
    )
  }

  return (
    <div
      className={className}
      style={{ position: 'absolute', inset: 0, transformStyle: 'preserve-3d' }}
    >
      {node}
    </div>
  )
}
