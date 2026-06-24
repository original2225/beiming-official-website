/**
 * 登录弹窗 — logo 放在右上角
 */

import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { login } from '../../api/modules/account'
import { ApiError } from '../../types/api'
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
      setError(e instanceof ApiError ? e.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative bg-white rounded-2xl w-[400px] shadow-2xl">
        <div className="px-8 pt-12 pb-8">
          {/* <img src={img('logo')} alt="" className="absolute -top-0 right-0 w-20 h-20 object-contain" /> */}

          <h2 className="text-xl font-bold text-gray-900">登录</h2>
          <p className="text-sm text-gray-500 mt-1 mb-6">登录你的北冥账号</p>

          {error && (
            <p className="text-sm text-red-600 mb-4 bg-red-50 px-3 py-2 rounded">{error}</p>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名" className="w-full px-4 py-2.5 bg-gray-100 rounded-lg text-gray-900 text-sm outline-none focus:ring-2 focus:ring-gray-400" required />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="密码" className="w-full px-4 py-2.5 bg-gray-100 rounded-lg text-gray-900 text-sm outline-none focus:ring-2 focus:ring-gray-400" required />
            <button type="submit" disabled={loading} className="w-full py-2.5 bg-gray-900 text-white rounded-lg text-sm font-medium hover:bg-gray-800 disabled:opacity-50 mt-2">
              {loading ? '登录中...' : '登录'}
            </button>
          </form>

          <p className="text-sm text-gray-400 text-center mt-4">
            还没有账号？{' '}
            <button onClick={onSwitchToRegister} className="text-gray-900 font-medium hover:underline">注册</button>
          </p>
        </div>
      </div>
    </div>
  )
}
