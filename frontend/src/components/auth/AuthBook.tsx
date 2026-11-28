/**
 * 登录 / 注册卡 —— 一页「上下都能翻」的活页纸（花蝉 2026-11-28 定的最终形态）。
 *
 * 版式（参照他给的两张活页本参考图）：
 *   ┌──────────────── 上排活页环
 *   │  上半：登录 / 注册表单       ← 绕【上边框】翻，翻开时表单向上翻走，
 *   │  背面是另一半表单（登录↔注册）
 *   ├╌╌╌ 中间分隔虚线
 *   │  下半：流动提示词 / 入服须知   ← 绕【下边框】翻，翻起来才看到完整守则条文
 *   └──────────────── 下排活页环
 *
 * 三个翻动都是「绕自己那条边框转」，不是整页换页：
 *   1. 上半翻转：在登录 / 注册之间切换（点「注册」向上翻，点「登录」翻回来）。
 *      翻动的是上半这一块本身，翻转 180° 时背面露出另一张表单。
 *   2. 下半翻起：点「入服须知」那一带，下半区绕它的下边框向上翻起来，露出条文。
 *
 * 踩过的坑（别再犯）：
 *   · 两页同时对转会视觉互穿 —— 只有「翻走的那一块」转，另一块静止。
 *   · 上下两块高度不同（注册多一个邀请码）—— 容器按较高者撑住，别让布局跳。
 *   · 整卡比视口高时整体缩放，别把活页环顶出屏幕。
 *   · 复位以 animationend 为主 + 经过时间轮询兜底：后台标签页动画不推进、
 *     setTimeout 会被节流，只靠定时器会把状态卡死。
 */

import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { LoginPageContent } from './LoginPageContent'
import { RegisterPageContent } from './RegisterPageContent'
import { EntryNoticeSheet } from './EntryNoticeSheet'

export type AuthPage = 'login' | 'register'

interface AuthBookProps {
  open: boolean
  page: AuthPage
  onClose: () => void
  onSwitchToPage: (page: AuthPage) => void
}

/** 上半翻页动画时长（ms），与 CSS 保持一致 */
const FLIP_MS = 620
/** 下半翻起动画时长（ms） */
const UNFOLD_MS = 480

/** 上半正在翻：翻动的是「哪一页的上半」，以及翻完露出的是哪一页 */
type Flipping = { from: AuthPage; to: AuthPage } | null

export function AuthBook({ open, page, onClose, onSwitchToPage }: AuthBookProps) {
  // 上半：正在翻动中（null = 静止）
  const [flipping, setFlipping] = useState<Flipping>(null)
  // 下半：是否翻起来（看到完整条文）
  const [noticeExpanded, setNoticeExpanded] = useState(false)

  const flipTimer = useRef<number | null>(null)
  const unfoldTimer = useRef<number | null>(null)

  const loginTopRef = useRef<HTMLDivElement | null>(null)
  const registerTopRef = useRef<HTMLDivElement | null>(null)
  // 上半两页的高度，容器按较高者撑住，翻页时不跳
  const [topHeights, setTopHeights] = useState({ login: 0, register: 0 })
  // 整卡缩放比：比视口高时整体缩小，别把活页环顶出屏幕
  const [scale, setScale] = useState(1)

  const clearFlipTimer = () => {
    if (flipTimer.current !== null) {
      window.clearTimeout(flipTimer.current)
      flipTimer.current = null
    }
  }
  const clearUnfoldTimer = () => {
    if (unfoldTimer.current !== null) {
      window.clearTimeout(unfoldTimer.current)
      unfoldTimer.current = null
    }
  }

  useEffect(() => () => { clearFlipTimer(); clearUnfoldTimer() }, [])

  // 量上半两页高度 + 算整卡缩放
  useLayoutEffect(() => {
    if (!open) return
    const measure = () => {
      const a = loginTopRef.current?.offsetHeight ?? 0
      const b = registerTopRef.current?.offsetHeight ?? 0
      if (a > 0 && b > 0) {
        setTopHeights((prev) => (prev.login === a && prev.register === b ? prev : { login: a, register: b }))
        const top = Math.max(a, b)
        // 上下活页杆各 26 + 下半折叠态 ≈ 96 + 上下留白 48
        const need = top + 26 * 2 + 96 + 48
        setScale(Math.max(0.72, Math.min(1, window.innerHeight / need)))
      }
    }
    measure()
    const t = window.setTimeout(measure, 140)
    return () => window.clearTimeout(t)
  }, [open, page])

  // 关弹窗复位
  useEffect(() => {
    if (!open) {
      clearFlipTimer()
      clearUnfoldTimer()
      setFlipping(null)
      setNoticeExpanded(false)
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
    const from = page
    setFlipping({ from, to: next })
    onSwitchToPage(next)
    scheduleReset(
      clearFlipTimer,
      () => setFlipping(null),
      FLIP_MS,
      (id) => { flipTimer.current = id },
    )
  }

  const toggleNotice = () => {
    const next = !noticeExpanded
    setNoticeExpanded(next)
    // 翻起来/收回去都要在动画结束后允许重排
    scheduleReset(
      clearUnfoldTimer,
      () => { /* 状态已设置，这里只需清定时器 */ },
      UNFOLD_MS,
      (id) => { unfoldTimer.current = id },
    )
  }

  const topHeight = Math.max(topHeights.login, topHeights.register)

  /** 上半两页的类名：正在翻的那页转，另一页静止；翻完目标页成为正面。 */
  const topClass = (leaf: AuthPage) => {
    if (flipping) {
      if (flipping.from === leaf) return 'bm-top-flap is-flipping-away'
      // 目标页：在背面等着，跟着一起转到正面
      return 'bm-top-flap is-flipping-in'
    }
    return page === leaf ? 'bm-top-flap is-front' : 'bm-top-flap is-back'
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

        <div className="bm-sheet">
          {/* ── 上半：表单，绕上边框翻 ── */}
          <div
            className="bm-top"
            style={topHeight ? { minHeight: topHeight } : undefined}
          >
            <div
              ref={loginTopRef}
              className={topClass('login')}
              aria-hidden={!(flipping ? flipping.to === 'login' : page === 'login')}
              onAnimationEnd={(e) => { if (e.target === e.currentTarget) setFlipping(null) }}
            >
              <LoginPageContent onClose={onClose} onSwitchToRegister={() => switchTo('register')} />
            </div>

            <div
              ref={registerTopRef}
              className={topClass('register')}
              aria-hidden={!(flipping ? flipping.to === 'register' : page === 'register')}
              onAnimationEnd={(e) => { if (e.target === e.currentTarget) setFlipping(null) }}
            >
              <RegisterPageContent onClose={onClose} onSwitchToLogin={() => switchTo('login')} />
            </div>
          </div>

          {/* ── 中间分隔虚线（可点：翻起下半看条文） ── */}
          <button
            type="button"
            onClick={toggleNotice}
            className={`bm-divider ${noticeExpanded ? 'is-open' : ''}`}
            aria-expanded={noticeExpanded}
            aria-controls="bm-notice-region"
          >
            <span className="bm-divider-label">入服须知 · 服务器守则与各服服规</span>
            <span className="bm-divider-chevron" aria-hidden="true">⌃</span>
          </button>

          {/* ── 下半：流动提示 / 入服须知，绕下边框翻 ── */}
          <div
            id="bm-notice-region"
            className={`bm-bottom ${noticeExpanded ? 'is-expanded' : ''}`}
            role="region"
            aria-label="入服须知"
          >
            <EntryNoticeSheet expanded={noticeExpanded} />
          </div>
        </div>

        {/* ── 下排活页环：挂在纸下边框 ── */}
        <div className="bm-rings bm-rings-bottom" aria-hidden="true">
          {Array.from({ length: 6 }).map((_, i) => <span key={i} className="bm-ring" />)}
        </div>
      </div>
    </div>
  )
}
