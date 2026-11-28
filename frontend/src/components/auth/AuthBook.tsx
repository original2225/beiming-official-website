/**
 * 登录 / 注册卡 —— 浅色活页纸，两页纸真实翻动（花蝉 2026-11-28 定稿 v6）。
 *
 *   ┌── ○ ○ ○ ○ ○ ○ ──  上排圆扣
 *   │  主活页：登录（含底部温馨提示）/ 注册   ← 绕上边框翻转
 *   └── ○ ○ ○ ○ ○ ○ ──  下排圆扣
 *
 * 只有一张纸、正反两面（花蝉 2026-11-28 定稿 v8）：
 *   · 登录页 = 正面。表单在上，剩余空白用「温馨提示」轮播填满（否则下方空一块）。
 *   · 注册页 = 反面。点「注册」整张纸绕上边框翻过去，翻下来的注册页把登录页
 *     连同底部的温馨提示一起盖住；点「登录」翻回去。
 *   · 只有一张纸在动，所以「盖住」是物理遮挡，不是靠层级硬压。
 *
 * 踩过的坑（别再改回去）：
 *   · 两页同时对转，转到 90° 时两页都不可见，会露出背景 → 只转一张。
 *   · 定位/尺寸必须写在基类；放进 is-* 状态类，切换状态那一刻会丢，两页一起塌回文档流。
 *   · 纸必须能翻到卡片轮廓外面：容器不能 overflow:hidden，否则翻到一半就被裁掉。
 *   · 背面不能用 opacity 藏起来 —— 真实纸张翻过去要能看到"纸背"，靠 backface-visibility。
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

/** 纸页翻动时长（ms），与 CSS 的 bm-leaf-in / bm-leaf-out 保持一致 */
const FLIP_MS = 620
/** 正在翻的纸：谁在动、往哪个方向。null = 静止 */
type Flipping = { leaf: AuthPage; dir: 'in' | 'out' } | null

export function AuthBook({ open, page, onClose, onSwitchToPage }: AuthBookProps) {
  // 上半：正在翻动中（null = 静止）
  const [flipping, setFlipping] = useState<Flipping>(null)
  const flipTimer = useRef<number | null>(null)

  // 整卡缩放比：比视口高时整体缩小，别把活页环顶出屏幕
  const [scale, setScale] = useState(1)

  const clearFlipTimer = () => {
    if (flipTimer.current !== null) {
      window.clearTimeout(flipTimer.current)
      flipTimer.current = null
    }
  }
  useEffect(() => () => { clearFlipTimer() }, [])

  // 算整卡缩放：卡片比视口高时整体缩小，别把活页环顶出屏幕。
  // 高度交给 CSS grid（两页叠在同一格，自动取较高者），这里只做整体缩放。
  useLayoutEffect(() => {
    if (!open) return
    const bookRef = { current: null as HTMLDivElement | null }
    const measure = () => {
      const el = bookRef.current ?? document.querySelector<HTMLDivElement>('.bm-book')
      if (!el) return
      const need = el.offsetHeight / (scale || 1) + 48
      setScale(Math.max(0.72, Math.min(1, window.innerHeight / need)))
    }
    measure()
    const t = window.setTimeout(measure, 160)
    return () => window.clearTimeout(t)
  }, [open, page])

  // 关弹窗复位
  useEffect(() => {
    if (!open) {
      clearFlipTimer()
      setFlipping(null)
    }
  }, [open])

  // 打开时锁背景滚动
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [open])

  if (!open) return null

  /** 用「经过时间轮询」兜底复位，避免后台标签页节流把状态卡死。 */
  const scheduleReset = (clear: () => void, done: () => void, ms: number, setRef: (id: number) => void) => {
    clear()
    const startedAt = performance.now()
    const tick = () => {
      if (performance.now() - startedAt >= ms) {
        done()
        return
      }
      setRef(window.setTimeout(tick, 60))
    }
    setRef(window.setTimeout(tick, 60))
  }

  const switchTo = (next: AuthPage) => {
    if (next === page) return
    // 动的是"要出现的那张纸"：注册页翻上来 = in；翻回登录 = 注册页翻回去 = out
    const dir: 'in' | 'out' = next === 'register' ? 'in' : 'out'
    const leaf: AuthPage = next === 'register' ? 'register' : 'register'
    setFlipping({ leaf, dir })
    onSwitchToPage(next)
    scheduleReset(
      clearFlipTimer,
      () => setFlipping(null),
      FLIP_MS,
      (id) => { flipTimer.current = id },
    )
  }

  /**
   * 整张纸的类名。
   *   · 静止：正面（登录）朝上 / 反面（注册）朝上。
   *   · 翻动：整张纸绕上边框转 180°，转到一半时纸会竖起来并跑到卡片外，
   *     翻完背面朝上 —— 这就是真纸翻页，不是换内容。
   */
  const leafClass = () => {
    if (flipping) {
      return `bm-leaf is-flipping ${flipping.dir === 'in' ? 'is-flipping-to-register' : 'is-flipping-to-login'}`
    }
    return `bm-leaf ${page === 'login' ? 'is-showing-login' : 'is-showing-register'}`
  }

  return (
    // 外层用 flex + 子元素 m-auto：内容矮时垂直居中；内容比视口高时（下半展开后
    // 会有 1200px+）从顶部开始并整体滚动。用 items-center 会把顶部直接顶出可视区，
    // 连上排活页环都看不到，所以这里不能那么写。
    <div className="fixed inset-0 z-50 flex overflow-y-auto py-6">
      <div className="auth-backdrop" onClick={onClose} />

      <div className="bm-book mx-auto my-auto" style={scale !== 1 ? { transform: `scale(${scale})` } : undefined}>
        {/* ── 上排活页环：挂在纸上边框 ── */}
        <div className="bm-rings bm-rings-top" aria-hidden="true">
          {Array.from({ length: 6 }).map((_, i) => <span key={i} className="bm-ring" />)}
        </div>

        {/* ── 主活页：一张纸，正面登录 / 反面注册。翻页 = 整张纸翻过去 ── */}
        <div className="bm-main">
          <div
            className={leafClass()}
            onAnimationEnd={(e) => { if (e.target === e.currentTarget) setFlipping(null) }}
          >
            <div className="bm-face is-login" aria-hidden={page !== 'login'}>
              <LoginPageContent onClose={onClose} onSwitchToRegister={() => switchTo('register')} />
            </div>
            <div className="bm-face is-register" aria-hidden={page !== 'register'}>
              <RegisterPageContent onClose={onClose} onSwitchToLogin={() => switchTo('login')} />
            </div>
          </div>
        </div>

        {/* ── 下排活页环：压在提示条下沿 ── */}
        <div className="bm-rings bm-rings-bottom" aria-hidden="true">
          {Array.from({ length: 6 }).map((_, i) => <span key={i} className="bm-ring" />)}
        </div>
      </div>
    </div>
  )
}
