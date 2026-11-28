/**
 * 登录页内容 —— 活页手册的第一页。
 *
 * 只是「页内容」，不带遮罩、不带外框：外框（固定杆 + 翻页动画）由 AuthBook 负责。
 * 表单字段与校验规则沿用原来的登录弹窗，没有改动。
 */

import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { login } from '../../api/modules/account'
import { ApiError } from '../../types/api'
import { describeApiError } from '../../api/errorMessages'
import { img } from '../../constants/images'
import { EntryNoticeSheet } from './EntryNoticeSheet'

interface LoginPageContentProps {
  onClose: () => void
  onSwitchToRegister: () => void
}

export function LoginPageContent({ onClose, onSwitchToRegister }: LoginPageContentProps) {
  const { login: setLogin } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

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

  return (
    <>
      <div className="px-8 pt-7 pb-5">
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
          <button
            type="button"
            onClick={onSwitchToRegister}
            className="font-medium hover:underline"
            style={{ color: '#2E5A6B' }}
          >
            注册
          </button>
        </p>
      </div>

      <EntryNoticeSheet />
    </>
  )
}