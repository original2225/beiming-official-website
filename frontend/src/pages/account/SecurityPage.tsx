/**
 * 账号安全页 — 修改密码、Minecraft 绑定/解绑
 */

import { useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { FormField } from '../../components/forms/FormField'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { useCurrentUser } from '../../hooks/useCurrentUser'
import { changePassword, bindMinecraft, unbindMinecraft } from '../../api/modules/account'
import { ApiError } from '../../types/api'

/** 游戏平台 —— 顺序按花蝉定的优先级：正版 > LittleSkin > 北冥皮肤站 > 离线 */
const PLATFORMS = [
  { value: 'MOJANG', label: 'Mojang 正版', hint: '买了正版、用微软账号登录的' },
  { value: 'LITTLESKIN', label: 'LittleSkin 外置登录', hint: '在 littleskin.cn 注册的外置账号' },
  { value: 'BEIMING', label: '北冥皮肤站', hint: '在 skinserver.beiming.games 注册的账号' },
  { value: 'OFFLINE', label: '离线服', hint: '整合包服这类离线入口，UUID 由昵称算出来' },
] as const

export function SecurityPage() {
  const user = useCurrentUser()
  const { run } = useRequest<unknown>()

  const [oldPwd, setOldPwd] = useState('')
  const [newPwd, setNewPwd] = useState('')
  const [pwdMsg, setPwdMsg] = useState('')

  const [mcName, setMcName] = useState('')
  const [mcPlatform, setMcPlatform] = useState<(typeof PLATFORMS)[number]['value']>('MOJANG')
  const [mcMsg, setMcMsg] = useState('')
  const [mcOk, setMcOk] = useState(false)

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    try { await run(() => changePassword({ oldPassword: oldPwd, newPassword: newPwd })); setPwdMsg('密码已修改') } catch (e) { setPwdMsg(e instanceof ApiError ? e.message : '修改失败') }
  }

  const handleBind = async () => {
    setMcOk(false)
    if (!/^[A-Za-z0-9_]{3,16}$/.test(mcName.trim())) {
      setMcMsg('游戏昵称需 3-16 位，只能用字母、数字、下划线')
      return
    }
    try {
      const res = await run(() => bindMinecraft({ minecraftName: mcName.trim(), minecraftPlatform: mcPlatform }))
      void res
      setMcMsg('绑定成功')
      setMcOk(true)
      setMcName('')
    } catch (e) {
      // 带上后端具体原因（43102 = 该平台查不到这个名字）
      setMcMsg(e instanceof ApiError ? e.message : '绑定失败')
    }
  }

  const handleUnbind = async () => {
    try { await run(() => unbindMinecraft()); setMcMsg('已解绑') } catch (e) { setMcMsg(e instanceof ApiError ? e.message : '解绑失败') }
  }

  return (
    <PageLayout variant="account">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="安" />
          <h1 className="font-display text-2xl text-indigo">账号安全</h1>
        </div>
        <InkStroke />
      </div>

      <div className="panel-ink p-4 mb-6">
        <h2 className="font-display text-sm text-ochre mb-3">修改密码</h2>
        <form onSubmit={handlePassword} className="flex flex-col gap-3 max-w-xs">
          <FormField label="当前密码">
            <input type="password" value={oldPwd} onChange={(e) => setOldPwd(e.target.value)} className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo" required />
          </FormField>
          <FormField label="新密码">
            <input type="password" value={newPwd} onChange={(e) => setNewPwd(e.target.value)} className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo" required />
            <span className="text-xs text-text-muted mt-1">6-128 位，至少含两类字符（字母、数字、符号任选两类）</span>
          </FormField>
          <button type="submit" className="btn-ink text-xs self-start">修改密码</button>
          {pwdMsg && <span className="text-xs text-jade">{pwdMsg}</span>}
        </form>
      </div>

      <div className="panel-ink p-4">
        <h2 className="font-display text-sm text-ochre mb-3">游戏账号绑定</h2>

        {user?.minecraftName ? (
          <div>
            <p className="text-sm mb-1">
              已绑定：<span className="text-indigo">{user.minecraftName}</span>
            </p>
            <p className="text-xs text-text-muted mb-3">
              平台：{PLATFORMS.find((p) => p.value === (user as { minecraftPlatform?: string }).minecraftPlatform)?.label ?? '未记录'}
            </p>
            <button onClick={handleUnbind} className="btn-ink-danger text-xs px-3 py-1">解绑</button>
          </div>
        ) : (
          <>
            <p className="text-xs text-text-muted mb-3">
              选择你在哪个平台玩，再填那个平台上的游戏昵称。服务端会去对应平台查这个昵称是否存在，
              查不到会明确告诉你——不会静默绑上一个查无此人的名字。
            </p>
            <div className="flex flex-col gap-3 max-w-md">
              <FormField label="游戏平台">
                <select
                  value={mcPlatform}
                  onChange={(e) => setMcPlatform(e.target.value as typeof mcPlatform)}
                  className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
                >
                  {PLATFORMS.map((p) => (
                    <option key={p.value} value={p.value}>{p.label}</option>
                  ))}
                </select>
                <span className="text-xs text-text-muted mt-1">
                  {PLATFORMS.find((p) => p.value === mcPlatform)?.hint}
                </span>
              </FormField>
              <div className="flex items-end gap-3">
                <FormField label="游戏昵称">
                  <input
                    type="text"
                    value={mcName}
                    onChange={(e) => setMcName(e.target.value)}
                    placeholder="3-16 位字母、数字或下划线"
                    className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
                  />
                </FormField>
                <button onClick={handleBind} className="btn-ink text-xs">绑定</button>
              </div>
            </div>
          </>
        )}

        {mcMsg && (
          <p className={`text-xs mt-3 ${mcOk ? 'text-jade' : 'text-cinnabar'}`}>{mcMsg}</p>
        )}

        <p className="text-xs text-text-muted mt-4 pt-3 border-t border-ink-600">
          正版服与 LittleSkin 服需要白名单，绑定后请联系管理按 <code className="text-ochre">multilogin whitelist add</code> 核对；
          北冥皮肤站与离线服不需要白名单。绑定只校验昵称存在，所有权以管理核对为准。
        </p>
      </div>
    </PageLayout>
  )
}
