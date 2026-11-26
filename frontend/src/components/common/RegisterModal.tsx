/**
 * 注册弹窗 — 和登录弹窗同款设计
 *
 * 三条体验修正（2026-11-26 花蝉验收时定的）：
 * 1. 补「显示名」输入框。后端 displayName 必填 2-24 位，前端原先拿 username 兜底，
 *    中文用户注册后会在成员列表里显示成一串英文 id。
 * 2. 注册成功后直接建立会话并关弹窗，不再多一步「请登录」。后端 register 本身就
 *    返回 accessToken，前端之前把它丢了等于白送一次会话又不用。
 * 3. 一次性邀请码注册成功后就地清空，避免用户反复提交撞「邀请码已用尽」。
 */

import { useState } from 'react'
import { useAuth } from '../../hooks/useAuth'
import { register } from '../../api/modules/account'
import { ApiError } from '../../types/api'
import { describeApiError } from '../../api/errorMessages'
import { img } from '../../constants/images'

/**
 * 与后端 AuthModule.validatePassword 对齐的密码判定。
 * 数出现了几类字符：大写、小写、数字、其他符号；要求至少两类。
 * 「abc123」「Abcdef」「abcd!@」都合法；纯数字「123456」和纯字母「abcdef」被拒。
 * 规则来自花蝉 2026-11-26：下限 6 位，任意两类字符即可。
 */
/**
 * 返回密码不合规的具体原因；合规时返回空串。
 * 抽出来是为了「边输边提示」和「提交校验」用同一套判断，
 * 不会出现输入时显示绿色、提交却说不行的情况。
 */
function getPasswordIssue(password: string): string {
  if (password.length === 0) return ''
  if (password.length < 6) return `还差 ${6 - password.length} 位（至少 6 位）`
  if (password.length > 128) return '太长了，最多 128 位'
  const classes = [
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length
  if (classes < 2) return '只有一类字符，再加数字、符号或大小写任意一种'
  return ''
}

function isPasswordAcceptable(password: string): boolean {
  return password.length >= 6 && password.length <= 128 && getPasswordIssue(password) === ''
}


interface RegisterModalProps {
  open: boolean
  onClose: () => void
  onSwitchToLogin: () => void
}

export function RegisterModal({ open, onClose, onSwitchToLogin }: RegisterModalProps) {
  const { login: setLogin } = useAuth()
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [invitationCode, setInvitationCode] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  // 密码规则不合规时的具体说明；为空串表示没问题。
  // 和提交时的校验共用同一套判断，避免「输入时说过、提交又说不过」。
  const passwordIssue = getPasswordIssue(password)

  const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault()
      setError('')
      // 后端规则：用户名 3-32 位字母数字下划线；显示名 2-24 位；
      // 密码 6-128 位且至少含两类字符（大写/小写/数字/符号任取两类）。
      // 前端照抄同一套规则先拦一道，用户不用提交一次才知道哪里不对。
      if (!/^[A-Za-z0-9_]{3,32}$/.test(username)) {
        setError('用户名需 3-32 位，只能用字母、数字、下划线')
        return
      }
      // 显示名后端必填 2-24 位。以前前端拿 username 兜底，中文用户注册完
      // 在成员列表里显示成一串英文 id，所以这里改成必须自己填。
      const trimmedDisplay = displayName.trim()
      if (trimmedDisplay.length < 2 || trimmedDisplay.length > 24) {
        setError('显示名需 2-24 个字')
        return
      }
      if (!isPasswordAcceptable(password)) {
        setError('密码需 6-128 位，且至少包含两类字符（字母、数字、符号任选两类）')
        return
      }
      if (invitationCode.trim().length === 0) {
        setError('请填写邀请码')
        return
      }
      setLoading(true)
      try {
        // 后端 register 直接返回 accessToken，用不着让用户再登录一次。
        const result = await register({
          username,
          displayName: trimmedDisplay,
          password,
          invitationCode: invitationCode.trim(),
        })
        // 一次性邀请码已经用掉了，清掉避免重复提交时撞「邀请码已用尽」
        setInvitationCode('')
        setPassword('')
        setLogin(result.token, result.user)
        // 弹窗别立刻消失：先说一句再关。否则用户会觉得「点了没反应」，
        // 分不清是成功了还是页面卡住。800ms 够读完这行字。
        setOk('注册成功，正在进入…')
        window.setTimeout(() => {
          onClose()
          setOk('')
        }, 800)
      } catch (e) {
        // 必须把后端的具体原因带出来（邀请码已用尽 / 用户名已占用 / 网络不通），
        // 只说「请稍后重试」等于什么都没说 —— 2026-11-26 花蝉验收时指出的。
        setError(e instanceof ApiError ? describeApiError(e.code, e.message) : '注册失败，请稍后重试')
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
            <input type="text" value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="显示名（2-24 个字，可填中文）" className="auth-input-light" required />
            <div className="flex flex-col gap-1">
              {/* 边输边校验：只在用户开始输入之后才提示，避免一打开弹窗就满屏红字 */}
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="密码（至少 6 位，含两类字符）"
                className="auth-input-light"
                style={
                  password.length > 0 && passwordIssue
                    ? { borderColor: '#B93A3A', boxShadow: '0 0 0 1px rgba(185,58,58,0.25)' }
                    : undefined
                }
                aria-invalid={password.length > 0 && !!passwordIssue}
                aria-describedby="pwd-hint"
                required
              />
              <span
                id="pwd-hint"
                className="text-xs"
                style={{
                  color: password.length === 0 ? '#6B7B85' : passwordIssue ? '#B93A3A' : '#3F7F68',
                }}
              >
                {password.length === 0
                  ? '至少 6 位，字母 / 数字 / 符号任选两类'
                  : passwordIssue
                    ? passwordIssue
                    : '密码可用'}
              </span>
            </div>
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
