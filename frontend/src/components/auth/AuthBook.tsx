/**
 * 活页手册外框 —— 登录 / 注册两张卡是同一本册子的两页（花蝉 2026-11-28 定的）。
 *
 * 结构照日历活页：
 *   顶部一根装订杆（带金属环），下面挂着一叠纸页。
 *   点「注册」→ 登录那页整页绕杆向上翻起、翻到杆后面去，露出下面的注册页。
 *   点「登录」→ 登录页从杆后面向下翻回来，盖住注册页。
 *
 * 翻页要点（踩过的坑）：
 *   1. 不能做成「A 页从 0 转到 -180、同时 B 页从 180 转到 0」——两页同时在转，
 *      视觉上是两张纸互相穿过，反而看不出翻页。只有一页转，另一页静止。
 *   2. 两页高度不同（登录少、注册多一个邀请码）。翻页时翻走的那页变成 absolute，
 *      容器高度会突然跳变，所以这里量两页实际高度，把容器撑到较高的那个。
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

/** 翻页动画时长（ms），与 CSS 的 transition / animation 保持一致 */
const FLIP_MS = 620

/** 正在翻的页 + 方向：up = 这页向上翻走；down = 这页从上面翻下来盖住下面 */
type Turning = { leaf: AuthPage; dir: 'up' | 'down' } | null

export function AuthBook({ open, page, onClose, onSwitchToPage }: AuthBookProps) {
  const [turning, setTurning] = useState<Turning>(null)
  const timerRef = useRef<number | null>(null)
  const loginRef = useRef<HTMLDivElement | null>(null)
  const registerRef = useRef<HTMLDivElement | null>(null)
  // 两页各自的实际高度。容器按较高者撑住（翻页不跳），垫在下面的页按当前页高度裁掉，
  // 否则注册页（比登录页高一个邀请码）会从登录页下方露出半截。
  const [heights, setHeights] = useState({ login: 0, register: 0 })
  // 整本册子的缩放比：卡片比视口高时整体缩小，别让装订杆被顶出屏幕
  const [scale, setScale] = useState(1)

  /** 翻页结束：清掉翻走页的标记。正常情况下由 animationend 触发，定时器只做兜底。 */
  const finishTurn = () => {
    clearTimer()
    setTurning(null)
  }

  const clearTimer = () => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }

  useEffect(() => clearTimer, [])

  // 量两页的自然高度，取较大值。两页都在 DOM 里（一页 absolute 垫着），所以都量得到。
  useLayoutEffect(() => {
    if (!open) return
    const measure = () => {
      const a = loginRef.current?.offsetHeight ?? 0
      const b = registerRef.current?.offsetHeight ?? 0
      if (a > 0 && b > 0) {
        setHeights((prev) => (prev.login === a && prev.register === b ? prev : { login: a, register: b }))
        // 装订杆 30 + 上留白 14 + 底边 7 ≈ 51；上下各留 12 就够，再多只是浪费可见面积
        const need = Math.max(a, b) + 51 + 24
        const available = window.innerHeight
        // 下限 0.7：再小表单就看不清了，那时宁可让外层滚动
        setScale(Math.max(0.7, Math.min(1, available / need)))
      }
    }
    measure()
    // 字体/图片加载完高度可能变，再量一次
    const t = window.setTimeout(measure, 120)
    return () => window.clearTimeout(t)
  }, [open, page])

  // 弹窗关掉时复位
  useEffect(() => {
    if (!open) {
      clearTimer()
      setTurning(null)
    }
  }, [open])

  // 弹窗打开时锁背景滚动，避免双滚动条与滚动穿透
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [open])

  if (!open) return null

  const switchTo = (next: AuthPage) => {
    if (next === page) return
    // 不因 turning 非空而死锁：上一次翻页的复位若被浏览器节流（后台标签页会把
    // setTimeout 推迟到 1s+），状态会一直挂着、按钮再也点不动。这里允许直接覆盖。
    clearTimer()
    if (next === 'register') {
      // 向前：登录页向上翻走
      setTurning({ leaf: 'login', dir: 'up' })
    } else {
      // 翻回来：登录页从上方向下翻下来盖住注册页
      setTurning({ leaf: 'login', dir: 'down' })
    }
    onSwitchToPage(next)
    clearTimer()
    // 复位方式：animationend 为主（最准）。但浏览器在后台标签页会暂停动画，
    // animationend 永远不来、setTimeout 也会被节流到 1s+，于是 turning 卡住。
    // 这里再补一条「点击后按真实经过时间收尾」的轮询，只跑 620ms 出头。
    const startedAt = performance.now()
    const tick = () => {
      if (performance.now() - startedAt >= FLIP_MS) {
        setTurning(null)
        return
      }
      timerRef.current = window.setTimeout(tick, 60)
    }
    timerRef.current = window.setTimeout(tick, 60)
  }

  const tallest = Math.max(heights.login, heights.register)
  const stackStyle = tallest ? { minHeight: tallest } : undefined

  /** 垫在下面的页只露出「当前页那么高」，多出来的部分裁掉。 */
  const underStyle = (leaf: AuthPage) => {
    const currentHeight = page === 'login' ? heights.login : heights.register
    const isUnder = !(turning && turning.leaf === leaf) && page !== leaf
    if (!isUnder || !currentHeight) return undefined
    return { height: currentHeight, overflow: 'hidden' as const }
  }

  /**
   * 页的类名。
   *
   * 注意「翻回来」(dir=down) 的语义：翻动的正是**登录页**，而它同时也是翻完后的当前页。
   * 所以不能把它当 is-under——那样动画期间就没有任何 is-current，上下文（例如按钮）
   * 会整个消失。这里把 down 的翻动页同时标成 current，它一边转一边就是首页。
   */
  const leafClass = (leaf: AuthPage) => {
    const base = 'bm-leaf'
    if (turning && turning.leaf === leaf) {
      return turning.dir === 'up'
        ? `${base} is-turning-up`
        : `${base} is-current is-turning-down`
    }
    if (page === leaf) return `${base} is-current`
    return `${base} is-under`
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto py-6">
      <div className="auth-backdrop" onClick={onClose} />

      <div className="bm-book mx-4" style={scale !== 1 ? { transform: `scale(${scale})` } : undefined}>
        {/* 顶部装订杆：横穿纸页上沿，翻起的页从它后面转过去 */}
        <div className="bm-book-bar" aria-hidden="true">
          <span className="bm-book-ring" />
          <span className="bm-book-ring" />
          <span className="bm-book-ring" />
          <span className="bm-book-ring" />
          <span className="bm-book-ring" />
          <span className="bm-book-ring" />
        </div>

        <div className="bm-book-stack" style={stackStyle}>
          <div
            ref={loginRef}
            className={leafClass('login')}
            style={underStyle('login')}
            aria-hidden={page !== 'login'}
            onAnimationEnd={(e) => { if (e.target === e.currentTarget) finishTurn() }}
          >
            <LoginPageContent onClose={onClose} onSwitchToRegister={() => switchTo('register')} />
          </div>

          <div
            ref={registerRef}
            className={leafClass('register')}
            style={underStyle('register')}
            aria-hidden={page !== 'register'}
            onAnimationEnd={(e) => { if (e.target === e.currentTarget) finishTurn() }}
          >
            <RegisterPageContent onClose={onClose} onSwitchToLogin={() => switchTo('login')} />
          </div>
        </div>

        {/* 册子底边：一叠纸的厚度 */}
        <div className="bm-book-edge" aria-hidden="true" />
      </div>
    </div>
  )
}
