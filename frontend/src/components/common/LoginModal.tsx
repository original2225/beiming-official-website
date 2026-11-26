/**
 * 登录弹窗 — logo 放在右上角
 */

import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { login } from '../../api/modules/account'
import { ApiError } from '../../types/api'
import { describeApiError } from '../../api/errorMessages'
import { img } from '../../constants/images'

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

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="auth-backdrop" onClick={onClose} />

      <div className="relative panel-rice w-[400px] mx-4">
        <div className="px-8 pt-10 pb-8">
          <img src={img('logo')} alt="" className="auth-logo absolute right-3 top-3" />

          <h2 className="text-xl font-display font-bold text-[#1F2A30]">登录</h2>
          <p className="text-sm text-[#6B7B85] mt-1 mb-6">登录你的北冥账号</p>

          {error && (
            <p className="text-sm mb-4 px-3 py-2 rounded-md" style={{ color: '#B93A3A', background: 'rgba(185,58,58,0.10)' }}>{error}</p>
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
      </div>
    </div>
  )
}
