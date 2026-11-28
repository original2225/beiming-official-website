/**
 * 登录 / 注册卡 —— 浅色活页纸，两张独立的纸叠放（花蝉 2026-11-28 定稿 v9）
 *
 *   ╭ ○ ○ ○ ○ ○ ○ ╮   ← 纸自己的上沿打出正圆孔（纸上有开口，不是画在外面的环）
 *   │  登录页        │  ← 上面这张
 *   ╰───────────────╯
 *   ╭ ○ ○ ○ ○ ○ ○ ╮
 *   │  注册页        │  ← 下面这张（平时被上面那张盖住，看不到）
 *   ╰───────────────╯
 *
 * 翻页语义（花蝉 2026-11-28 纠正过的关键点）：
 *   · 是【两张纸】，不是一张纸正反翻。
 *   · 点「注册」：把【上面这张】（登录页）绕上边框翻走，翻到卡片外、落到背后，
 *     露出本来就压在下面的注册页。
 *   · 点「登录」：把那张纸翻回来，重新盖住注册页。
 *   · 所以永远只有「当前这一张」在动，另一张老老实实躺在原地等着露出来。
 *
 * 踩过的坑（别再改回去）：
 *   · 两页同时对转，转到 90° 时两页都不可见，会露出背景 → 只转要走的那一张。
 *   · 定位/尺寸写在基类；放进状态类会在切状态那一帧丢失，两页一起塌回文档流。
 *   · 容器不能 overflow:hidden，否则纸翻到一半就被裁掉。
 *   · 绕上边框转 180° 后纸会镜像到卡片上方，必须补 translateY(-100%) 且写在 rotate 之后。
 */

import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { LoginPageContent } from './LoginPageContent'
import { RegisterPageContent } from './RegisterPageContent'

export type AuthPage = 'login' | 'register'

interface AuthBookProps {
  open: boolean
  page: AuthPage
  onClose: () => void
  onSwitchToPage: (page: AuthPage) => void
}

/** 纸页翻动时长（ms），与 CSS 关键帧一致 */
const FLIP_MS = 700

/**
 * 正在翻的纸。
 *   away = 把当前这张翻走（点注册时：翻走登录页）
 *   back = 把翻走的那张翻回来（点登录时）
 */
type Flipping = { dir: 'away' | 'back'; mover: AuthPage } | null

/** 每张纸的上沿打 6 个正圆孔 */
const HOLE_COUNT = 6

export function AuthBook({ open, page, onClose, onSwitchToPage }: AuthBookProps) {
  const [flipping, setFlipping] = useState<Flipping>(null)
  const flipTimer = useRef<number | null>(null)
  const [scale, setScale] = useState(1)

  const clearFlipTimer = () => {
    if (flipTimer.current !== null) {
      window.clearTimeout(flipTimer.current)
      flipTimer.current = null
    }
  }
  useEffect(() => () => { clearFlipTimer() }, [])

  // 卡片比视口高时整体缩小，别把纸顶出屏幕
  useLayoutEffect(() => {
    if (!open) return
    const measure = () => {
      const el = document.querySelector<HTMLDivElement>('.bm-book')
      if (!el) return
      const need = el.offsetHeight / (scale || 1) + 40
      setScale(Math.max(0.72, Math.min(1, window.innerHeight / need)))
    }
    measure()
    const t = window.setTimeout(measure, 160)
    return () => window.clearTimeout(t)
  }, [open, page])

  useEffect(() => {
    if (!open) {
      clearFlipTimer()
      setFlipping(null)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [open])

  if (!open) return null

  /** 用「经过时间轮询」兜底复位，避免后台标签页节流把状态卡死。 */
  const scheduleReset = (done: () => void, ms: number) => {
    clearFlipTimer()
    const startedAt = performance.now()
    const tick = () => {
      if (performance.now() - startedAt >= ms) {
        done()
        return
      }
      flipTimer.current = window.setTimeout(tick, 60)
    }
    flipTimer.current = window.setTimeout(tick, 60)
  }

  const switchTo = (next: AuthPage) => {
    if (next === page) return
    // 点注册：把上面这张（登录）翻走；点登录：把它翻回来。动的永远是登录那张。
    const mover: AuthPage = 'login'
    const dir: 'away' | 'back' = next === 'register' ? 'away' : 'back'
    setFlipping({ dir, mover })
    onSwitchToPage(next)
    scheduleReset(() => setFlipping(null), FLIP_MS)
  }

  /**
   * 每张纸的类名。
   *   静止时：当前页 is-top（在上面），另一页 is-under（压在下面看不见）。
   *   翻动时：只有 mover 那张拿翻动类，另一张保持 is-top / is-under 不动。
   */
  const pageClass = (leaf: AuthPage) => {
    if (flipping && flipping.mover === leaf) {
      return `bm-page ${flipping.dir === 'away' ? 'is-turning-away' : 'is-turning-back'}`
    }
    return page === leaf ? 'bm-page is-top' : 'bm-page is-under'
  }

  const renderHoles = () => (
    <div className="bm-holes" aria-hidden="true">
      {Array.from({ length: HOLE_COUNT }).map((_, i) => <span key={i} className="bm-hole" />)}
    </div>
  )

  return (
    <div className="fixed inset-0 z-50 flex overflow-y-auto py-6">
      <div className="auth-backdrop" onClick={onClose} />

      <div className="bm-book mx-auto my-auto" style={scale !== 1 ? { transform: `scale(${scale})` } : undefined}>
        <div className="bm-stack">
          {/* 下面那张：注册页。平时被上面那张完全盖住 */}
          <div
            className={pageClass('register')}
            aria-hidden={page !== 'register'}
            onAnimationEnd={(e) => { if (e.target === e.currentTarget) setFlipping(null) }}
          >
            {renderHoles()}
            <RegisterPageContent onClose={onClose} onSwitchToLogin={() => switchTo('login')} />
          </div>

          {/* 上面那张：登录页。翻走/翻回的都是它 */}
          <div
            className={pageClass('login')}
            aria-hidden={page !== 'login'}
            onAnimationEnd={(e) => { if (e.target === e.currentTarget) setFlipping(null) }}
          >
            {renderHoles()}
            <LoginPageContent onClose={onClose} onSwitchToRegister={() => switchTo('register')} />
          </div>
        </div>
      </div>
    </div>
  )
}
