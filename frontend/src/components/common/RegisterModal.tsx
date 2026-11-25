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
      // 后端规则：用户名 3-32 位字母数字下划线；密码 10-128 位且必须同时含字母和数字；
      // 显示名 2-24 位。此前前端一概不校验也不提示，用户只能拿到一句 "invalid request"，
      // 完全不知道该改哪里（实测 9 位密码就会这样）。
      if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) {
        setError('用户名需 3-32 位，只能用字母、数字、下划线')
        return
      }
      if (password.length < 10 || password.length > 128 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
        setError('密码需 10-128 位，且同时包含字母和数字')
        return
      }
      if (invitationCode.trim().length === 0) {
        setError('请填写邀请码')
        return
      }
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
      <div className="auth-backdrop" onClick={onClose} />

      <div className="relative panel-rice w-[400px] mx-4">
        <div className="px-8 pt-10 pb-8">
          <img src={img('logo')} alt="" className="auth-logo absolute right-3 top-3" />

          <h2 className="text-xl font-display font-bold text-[#1F2A30]">注册</h2>
          <p className="text-sm text-[#6B7B85] mt-1 mb-6">创建你的北冥账号</p>

          {error && <p className="text-sm mb-4 px-3 py-2 rounded-md" style={{ color: '#B93A3A', background: 'rgba(185,58,58,0.10)' }}>{error}</p>}
          {ok && <p className="text-sm mb-4 px-3 py-2 rounded-md" style={{ color: '#3F7F68', background: 'rgba(74,155,127,0.12)' }}>{ok}</p>}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="用户名（3-32 位，字母/数字/下划线）" className="auth-input-light" required />
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="密码（至少 10 位，含字母和数字）" className="auth-input-light" required />
            <input type="text" value={invitationCode} onChange={(e) => setInvitationCode(e.target.value)} placeholder="邀请码（如 PLAYER-CODE-1）" className="auth-input-light" required />
            <button type="submit" disabled={loading} className="btn-auth-light mt-2">
              {loading ? '注册中...' : '注册'}
            </button>
          </form>

          <p className="text-sm text-[#6B7B85] text-center mt-4">
            已有账号？{' '}
            <button onClick={onSwitchToLogin} className="font-medium hover:underline" style={{ color: '#2E5A6B' }}>登录</button>
          </p>
        </div>
      </div>
    </div>
  )
}
