/**
 * 登录弹窗 — logo 放在右上角；下沿接一块可翻开的「入服须知」书页。
 *
 * 花蝉 2026-11-28 要的交互：点登录卡下沿那一条，卡片下半截像便利贴 / 书页一样翻开，
 * 翻出来的是「入服须知」——服务器守则、各服服规、入服指南轮着播；上半（标题到密码）保持不动。
 * 内容口径：服务器守则 = 全体通用条约；服规 = 某个（某类）服务器专用（生电服规只原生电服、
 * 愚者服规只原愚者服），文案在 constants/rulesContent.ts，取自两份服规原文，未加条款。
 */

import { useEffect, useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { login } from '../../api/modules/account'
import { ApiError } from '../../types/api'
import { describeApiError } from '../../api/errorMessages'
import { img } from '../../constants/images'
import { RULE_GROUPS, ROTATING_NOTES } from '../../constants/rulesContent'

interface LoginModalProps {
  open: boolean
  onClose: () => void
  onSwitchToRegister: () => void
}

export function LoginModal({ open, onClose, onSwitchToRegister }: LoginModalProps) {
  const { login: setLogin } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  // 书页是否翻开
  const [noticeOpen, setNoticeOpen] = useState(false)
  // 轮播到第几条注意事项
  const [noteIndex, setNoteIndex] = useState(0)

  // 书页打开时每 4 秒换一条注意事项；关掉就停，别在后台空转。
  useEffect(() => {
    if (!open || !noticeOpen) return
    const timer = window.setInterval(() => {
      setNoteIndex((i) => (i + 1) % ROTATING_NOTES.length)
    }, 4000)
    return () => window.clearInterval(timer)
  }, [open, noticeOpen])

  // 关弹窗时把书页复位，下次打开是合上的
  useEffect(() => {
    if (!open) {
      setNoticeOpen(false)
      setNoteIndex(0)
    }
  }, [open])

  // 弹窗打开时锁住背景滚动：否则书页内容一长会出现「页面滚动条 + 弹窗内滚动条」双滚动，
  // 还容易滚动穿透到背后的首页。关掉或卸载时恢复。
  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [open])

  if (!open) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const result = await login({ username, password })
      setLogin(result.token, result.user)
      onClose()
    } catch (e) {
      setError(e instanceof ApiError ? describeApiError(e.code, e.message) : '登录失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const note = ROTATING_NOTES[noteIndex]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto py-6">
      <div className="auth-backdrop" onClick={onClose} />

      <div className="relative panel-rice w-[400px] mx-4 my-auto">
        {/* ── 上半：标题到密码保持不动 ── */}
        <div className="px-8 pt-10 pb-5">
          <img src={img('logo')} alt="" className="auth-logo absolute right-3 top-3" />

          <h2 className="text-xl font-display font-bold text-[#1F2A30]">登录</h2>
          <p className="text-sm text-[#6B7B85] mt-1 mb-6">登录你的北冥账号</p>

          {error && (
            <div
              key={error}
              className="bm-error-alert text-sm mb-4 px-3 py-2 rounded-md flex items-start gap-2"
              style={{ color: '#B93A3A', background: 'rgba(185,58,58,0.10)', borderLeft: '3px solid #B93A3A' }}
              role="alert"
              aria-live="assertive"
            >
              <span aria-hidden="true" className="bm-error-icon shrink-0">!</span>
              <span className="flex-1">{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名" className="auth-input-light" required />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="密码" className="auth-input-light" required />
            <button type="submit" disabled={loading} className="btn-auth-light mt-2">
              {loading ? '登录中...' : '登录'}
            </button>
          </form>

          <p className="text-sm text-[#6B7B85] text-center mt-4">
            还没有账号？{' '}
            <button onClick={onSwitchToRegister} className="font-medium hover:underline" style={{ color: '#2E5A6B' }}>注册</button>
          </p>
        </div>

        {/* ── 下半：可翻开的「入服须知」书页 ── */}
        <div className={`bm-notice ${noticeOpen ? 'is-open' : ''}`}>
          <button
            type="button"
            onClick={() => setNoticeOpen((v) => !v)}
            className="bm-notice-tab"
            aria-expanded={noticeOpen}
            aria-controls="bm-notice-body"
          >
            <span className="bm-notice-tab-left">
              <span className="bm-notice-tab-title">入服须知</span>
              <span className="bm-notice-tab-sub">服务器守则 · 各服服规 · 入服指南</span>
            </span>
            <span className={`bm-notice-chevron ${noticeOpen ? 'open' : ''}`} aria-hidden="true">⌄</span>
          </button>

          <div id="bm-notice-body" className="bm-notice-body" role="region" aria-label="入服须知">
            <div className="bm-notice-clip">
              <div className="bm-notice-inner">
                <div className="bm-note-ticker" key={noteIndex}>
                  <span className="bm-note-tag">{note.tag}</span>
                  <span className="bm-note-text">{note.text}</span>
                </div>
                <p className="bm-notice-scope">
                  <strong>服务器守则</strong>是全体玩家都要遵守的条约；<strong>服规</strong>是某个（某类）服务器专用的——
                  生电服服规只管原版生存服、愚者服服规只管愚者服。各服务器页面里有对应服规和玩家指南。
                </p>

                {RULE_GROUPS.map((group) => (
                  <div key={group.key} className="bm-rule-group">
                    <div className="bm-rule-head">
                      <span className={`bm-rule-scope scope-${group.key}`}>{group.scope}</span>
                      <span className="bm-rule-title">{group.title}</span>
                    </div>
                    <ul className="bm-rule-list">
                      {group.items.map((item, i) => (
                        <li key={i}>{item}</li>
                      ))}
                    </ul>
                  </div>
                ))}

                <p className="bm-notice-foot">完整条文见「指南中心」的服务器守则与各服服规。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
