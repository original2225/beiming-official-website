/**
 * 注册弹窗 — 和登录弹窗同款设计
 *
 * 字段顺序（花蝉 2026-11-27 定的）：
 *   昵称（中文，社区里显示的名字）
 *   → 用户名 / 游戏昵称（同一个值，既是登录名也是 Minecraft 账户名）
 *   → 密码 → 邀请码 → 游戏平台 → 注册
 *
 * 为什么用户名和 MC 昵称合成一个：
 * 原来的流程要用户自己填 32 位 UUID 才能绑 MC，普通玩家根本不知道自己的
 * UUID，这个功能等于不可用。改成「用户名就是游戏昵称」，服务端按平台
 * （LittleSkin / 正版 / 离线服）自动查 UUID，注册完即完成绑定。
 *
 * 另三条体验修正（2026-11-26 花蝉验收时定的）：
 * 1. 补「昵称」输入框，后端 displayName 必填 2-24 位；以前前端拿 username
 *    兜底，中文用户注册后在成员列表里显示成一串英文 id。现在昵称提到第一位。
 * 2. 注册成功后直接建立会话并关弹窗，不再多一步「请登录」。后端 register
 *    本身就返回 accessToken，前端之前把它丢了等于白送一次会话又不用。
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


/** 游戏平台：决定服务端去哪里查 UUID */
const PLATFORMS = [
  { value: 'LITTLESKIN', label: 'LittleSkin 外置登录', hint: '在 littleskin.cn 注册的外置账号' },
  { value: 'MOJANG', label: 'Mojang 正版', hint: '买了正版、用微软账号登录的' },
  { value: 'OFFLINE', label: '离线服', hint: '北冥离线服，UUID 由昵称算出来' },
] as const

interface RegisterModalProps {
  open: boolean
  onClose: () => void
  onSwitchToLogin: () => void
}

export function RegisterModal({ open, onClose, onSwitchToLogin }: RegisterModalProps) {
  const { login: setLogin } = useAuth()
  // 昵称提到第一位：社区里显示的中文名字
  const [displayName, setDisplayName] = useState('')
  // 用户名即游戏昵称，同一份数据，不再分两个字段
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [invitationCode, setInvitationCode] = useState('')
  const [platform, setPlatform] = useState<(typeof PLATFORMS)[number]['value']>('LITTLESKIN')
  const [error, setError] = useState('')
  // 记录是哪一类错误，用来把对应的输入框标红（报错条在最上面，
  // 光有文字的话用户不知道是哪一格出的问题）
  const [errorField, setErrorField] = useState<'' | 'displayName' | 'username' | 'password' | 'invitation'>('')
  const [ok, setOk] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  // 密码规则不合规时的具体说明；为空串表示没问题。
  // 和提交时的校验共用同一套判断，避免「输入时说过、提交又说不过」。
  const passwordIssue = getPasswordIssue(password)
  // 用户名即 MC 昵称，规则同 Minecraft：3-16 位字母数字下划线
  const nameIssue =
    username.length === 0
      ? ''
      : !/^[A-Za-z0-9_]{3,16}$/.test(username)
        ? '游戏昵称需 3-16 位，只能用字母、数字、下划线'
        : ''

  const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault()
      setError('')
      setErrorField('')
      // 昵称（中文）—— 社区里显示的名字，后端 2-24 位必填
      const trimmedDisplay = displayName.trim()
      if (trimmedDisplay.length < 2 || trimmedDisplay.length > 24) {
        setErrorField('displayName')
        setError('昵称需 2-24 个字（可填中文）')
        return
      }
      // 用户名 = 游戏昵称，规则与 Minecraft 一致（3-16 位字母数字下划线）。
      // 后端 username 规则宽到 32 位，但这里收紧到 MC 的 16 位，因为二者是同一个值。
      if (!/^[A-Za-z0-9_]{3,16}$/.test(username)) {
        setErrorField('username')
        setError('游戏昵称需 3-16 位，只能用字母、数字、下划线')
        return
      }
      if (!isPasswordAcceptable(password)) {
        setErrorField('password')
        setError('密码需 6-128 位，且至少包含两类字符（字母、数字、符号任选两类）')
        return
      }
      if (invitationCode.trim().length === 0) {
        setErrorField('invitation')
        setError('请填写邀请码')
        return
      }
      setLoading(true)
      try {
        // 后端 register 直接返回 accessToken，用不着让用户再登录一次。
        // minecraftName 与用户名同值：服务端按 minecraftPlatform 查 UUID 并绑定，
        // 用户不需要知道自己的 UUID 是多少。
        const result = await register({
          username,
          displayName: trimmedDisplay,
          password,
          invitationCode: invitationCode.trim(),
          minecraftName: username,
          minecraftPlatform: platform,
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
        if (e instanceof ApiError) {
          // 按错误码反推是哪个字段的问题，好把那一格标红
          const isInvitation = [43101, 43112, 43113, 43114].includes(e.code)
          setErrorField(isInvitation ? 'invitation' : '')
          setError(describeApiError(e.code, e.message))
        } else {
          setErrorField('')
          setError('注册失败，请稍后重试')
        }
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

          {/* 报错：抖动 + 左侧红条 + 警示图标。
              花蝉 2026-11-27 要的「最后加点动画警示一下」——
              光换个颜色不够醒目，要让人一眼看到自己被打回来了。
              key={error} 保证同一句话再次触发时会重新播放动画。 */}
          {error && (
            <div
              key={error}
              className="bm-error-alert text-sm mb-4 px-3 py-2 rounded-md flex items-start gap-2"
              style={{
                color: '#B93A3A',
                background: 'rgba(185,58,58,0.10)',
                borderLeft: '3px solid #B93A3A',
              }}
              role="alert"
              aria-live="assertive"
            >
              <span aria-hidden="true" className="bm-error-icon shrink-0">
                !
              </span>
              <span className="flex-1">{error}</span>
            </div>
          )}
          {ok && <p className="text-sm mb-4 px-3 py-2 rounded-md" style={{ color: '#3F7F68', background: 'rgba(74,155,127,0.12)' }}>{ok}</p>}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {/* 1. 昵称：社区里显示的名字，中文优先 */}
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="昵称（2-24 个字，中文即可）"
              className="auth-input-light"
              style={errorField === 'displayName' ? { borderColor: '#B93A3A', boxShadow: '0 0 0 1px rgba(185,58,58,0.25)' } : undefined}
              aria-invalid={errorField === 'displayName'}
              required
            />

            {/* 2. 用户名 = 游戏昵称：既是登录名，也是 Minecraft 账户名 */}
            <div className="flex flex-col gap-1">
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="游戏昵称（3-16 位，字母/数字/下划线）"
                className="auth-input-light"
                style={
                  (username.length > 0 && nameIssue) || errorField === 'username'
                    ? { borderColor: '#B93A3A', boxShadow: '0 0 0 1px rgba(185,58,58,0.25)' }
                    : undefined
                }
                aria-invalid={(username.length > 0 && !!nameIssue) || errorField === 'username'}
                required
              />
              <span className="text-xs" style={{ color: nameIssue ? '#B93A3A' : '#6B7B85' }}>
                {nameIssue || '这个昵称同时是你的登录名和游戏账号名'}
              </span>
            </div>

            {/* 3. 密码 */}
            <div className="flex flex-col gap-1">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="密码（至少 6 位，含两类字符）"
                className="auth-input-light"
                style={
                  (password.length > 0 && passwordIssue) || errorField === 'password'
                    ? { borderColor: '#B93A3A', boxShadow: '0 0 0 1px rgba(185,58,58,0.25)' }
                    : undefined
                }
                aria-invalid={(password.length > 0 && !!passwordIssue) || errorField === 'password'}
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
            <input
              type="text"
              value={invitationCode}
              onChange={(e) => setInvitationCode(e.target.value)}
              placeholder="邀请码（如 PLAYER-CODE-1）"
              className="auth-input-light"
              // 邀请码出错时把这一格也标红：报错条在表单顶部，离这一格有好几行，
              // 只写文字的话用户得自己找是哪一格的问题。
              style={errorField === 'invitation' ? { borderColor: '#B93A3A', boxShadow: '0 0 0 1px rgba(185,58,58,0.25)' } : undefined}
              aria-invalid={errorField === 'invitation'}
              required
            />

            {/* 5. 游戏平台：服务端据此去查你的 UUID */}
            <div className="flex flex-col gap-1">
              <label className="text-xs text-[#6B7B85]">你的游戏账号在哪个平台</label>
              <select
                value={platform}
                onChange={(e) => setPlatform(e.target.value as typeof platform)}
                className="auth-input-light"
              >
                {PLATFORMS.map((p) => (
                  <option key={p.value} value={p.value}>
                    {p.label}
                  </option>
                ))}
              </select>
              <span className="text-xs text-[#6B7B85]">
                {PLATFORMS.find((p) => p.value === platform)?.hint}
              </span>
            </div>

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
