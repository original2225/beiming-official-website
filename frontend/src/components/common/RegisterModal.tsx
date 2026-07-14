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
      <div className="absolute inset-0 bg-black/60" onClick={onClose} />

      <div className="relative panel-ink w-[400px] mx-4">
        <div className="px-8 pt-10 pb-8">
          <img src={img('logo')} alt="" className="absolute -top-0 right-0 w-20 h-20 object-contain opacity-80" />

          <h2 className="text-xl font-display font-bold text-text-primary">注册</h2>
          <p className="text-sm text-text-muted mt-1 mb-6">创建你的北冥账号</p>

          {error && <p className="text-sm text-cinnabar mb-4 bg-cinnabar/10 px-3 py-2 rounded-md">{error}</p>}
          {ok && <p className="text-sm text-jade mb-4 bg-jade/10 px-3 py-2 rounded-md">{ok}</p>}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名" className="w-full px-4 py-2.5 bg-surface-light rounded-md text-text-primary text-sm outline-none focus:ring-2 focus:ring-indigo" required />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="密码" className="w-full px-4 py-2.5 bg-surface-light rounded-md text-text-primary text-sm outline-none focus:ring-2 focus:ring-indigo" required />
            <input type="text" value={invitationCode} onChange={(e) => setInvitationCode(e.target.value)} placeholder="邀请码" className="w-full px-4 py-2.5 bg-surface-light rounded-md text-text-primary text-sm outline-none focus:ring-2 focus:ring-indigo" required />
            <button type="submit" disabled={loading} className="btn-ink w-full text-sm disabled:opacity-50 mt-2">
              {loading ? '注册中...' : '注册'}
            </button>
          </form>

          <p className="text-sm text-text-muted text-center mt-4">
            已有账号？{' '}
            <button onClick={onSwitchToLogin} className="text-indigo font-medium hover:underline">登录</button>
          </p>
        </div>
      </div>
    </div>
  )
}
