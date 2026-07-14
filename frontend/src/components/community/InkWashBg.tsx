/**
 * 社区浅色主题下的水墨晕染背景
 * 包含：宣纸底纹、墨色/赭石/黛蓝晕染、边角水墨山形装饰
 */

export function InkWashBg() {
  return (
    <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
      {/* 宣纸纹理 */}
      <svg
        className="absolute inset-0 w-full h-full opacity-[0.08]"
        xmlns="http://www.w3.org/2000/svg"
      >
        <filter id="paper-noise" x="0" y="0" width="100%" height="100%">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.8"
            numOctaves="3"
            stitchTiles="stitch"
          />
        </filter>
        <rect width="100%" height="100%" filter="url(#paper-noise)" />
      </svg>

      {/* 浓墨晕染 blob */}
      <div
        className="ink-blob bg-indigo/20 w-[44rem] h-[44rem] -top-24 -left-24"
        style={{ animationDelay: '0s' }}
      />
      <div
        className="ink-blob bg-ochre/15 w-[36rem] h-[36rem] top-[25%] -right-24"
        style={{ animationDelay: '-7s' }}
      />
      <div
        className="ink-blob bg-ink-600/10 w-[52rem] h-[52rem] -bottom-32 left-[15%]"
        style={{ animationDelay: '-14s' }}
      />

      {/* 右下角淡墨山形 */}
      <svg
        viewBox="0 0 400 300"
        className="absolute -bottom-10 -right-10 w-[28rem] h-[21rem] text-ink-600/10"
        preserveAspectRatio="xMidYMid slice"
      >
        <path
          d="M0,300 C60,240 120,220 180,260 C240,300 300,200 400,180 L400,300 Z"
          fill="currentColor"
        />
      </svg>

      {/* 左上角水墨笔触 */}
      <svg
        viewBox="0 0 300 200"
        className="absolute -top-10 -left-10 w-[20rem] h-[14rem] text-ink-600/8 rotate-12"
        preserveAspectRatio="xMidYMid slice"
      >
        <path
          d="M0,0 C80,40 160,20 300,60 L300,0 Z"
          fill="currentColor"
        />
      </svg>
    </div>
  )
}
