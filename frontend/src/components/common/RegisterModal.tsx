/**
 * 注册弹窗 — 和登录弹窗同款设计
 */

import { useState } from 'react'
import { register } from '../../api/modules/account'
import { ApiError } from '../../types/api'
import { img } from '../../constants/images'

interface RegisterModalProps {
  open: boolean
  onClose: () => void
  onSwitchToLogin: () => void
}

export function RegisterModal({ open, onClose, onSwitchToLogin }: RegisterModalProps) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [invitationCode, setInvitationCode] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setOk('')
    setLoading(true)
    try {
      await register({ username, password, invitationCode })
      setOk('注册成功，请登录')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      <div className="relative bg-white rounded-2xl w-[400px] shadow-2xl">
        <div className="px-8 pt-12 pb-8">
          <img src={img('logo')} alt="" className="absolute -top-0 right-0 w-20 h-20 object-contain" />

          <h2 className="text-xl font-bold text-gray-900">注册</h2>
          <p className="text-sm text-gray-500 mt-1 mb-6">创建你的北冥账号</p>

          {error && <p className="text-sm text-red-600 mb-4 bg-red-50 px-3 py-2 rounded">{error}</p>}
          {ok && <p className="text-sm text-green-600 mb-4 bg-green-50 px-3 py-2 rounded">{ok}</p>}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名" className="w-full px-4 py-2.5 bg-gray-100 rounded-lg text-gray-900 text-sm outline-none focus:ring-2 focus:ring-gray-400" required />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="密码" className="w-full px-4 py-2.5 bg-gray-100 rounded-lg text-gray-900 text-sm outline-none focus:ring-2 focus:ring-gray-400" required />
            <input type="text" value={invitationCode} onChange={(e) => setInvitationCode(e.target.value)} placeholder="邀请码" className="w-full px-4 py-2.5 bg-gray-100 rounded-lg text-gray-900 text-sm outline-none focus:ring-2 focus:ring-gray-400" required />
            <button type="submit" disabled={loading} className="w-full py-2.5 bg-gray-900 text-white rounded-lg text-sm font-medium hover:bg-gray-800 disabled:opacity-50 mt-2">
              {loading ? '注册中...' : '注册'}
            </button>
          </form>

          <p className="text-sm text-gray-400 text-center mt-4">
            已有账号？{' '}
            <button onClick={onSwitchToLogin} className="text-gray-900 font-medium hover:underline">登录</button>
          </p>
        </div>
      </div>
    </div>
  )
}
