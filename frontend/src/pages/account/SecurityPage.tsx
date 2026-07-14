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

export function SecurityPage() {
  const user = useCurrentUser()
  const { run } = useRequest<unknown>()

  const [oldPwd, setOldPwd] = useState('')
  const [newPwd, setNewPwd] = useState('')
  const [pwdMsg, setPwdMsg] = useState('')

  const [mcName, setMcName] = useState('')
  const [mcMsg, setMcMsg] = useState('')

  const handlePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    try { await run(() => changePassword({ oldPassword: oldPwd, newPassword: newPwd })); setPwdMsg('密码已修改') } catch (e) { setPwdMsg(e instanceof ApiError ? e.message : '修改失败') }
  }

  const handleBind = async () => {
    try { await run(() => bindMinecraft({ minecraftName: mcName })); setMcMsg('绑定已提交') } catch (e) { setMcMsg(e instanceof ApiError ? e.message : '绑定失败') }
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
          </FormField>
          <button type="submit" className="btn-ink text-xs self-start">修改密码</button>
          {pwdMsg && <span className="text-xs text-jade">{pwdMsg}</span>}
        </form>
      </div>

      <div className="panel-ink p-4">
        <h2 className="font-display text-sm text-ochre mb-3">Minecraft 绑定</h2>
        {user?.minecraftName ? (
          <div>
            <p className="text-sm mb-2">已绑定: <span className="text-indigo">{user.minecraftName}</span></p>
            <button onClick={handleUnbind} className="btn-ink-danger text-xs px-3 py-1">解绑</button>
          </div>
        ) : (
          <div className="flex items-end gap-3">
            <FormField label="Minecraft 用户名">
              <input type="text" value={mcName} onChange={(e) => setMcName(e.target.value)} className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo" />
            </FormField>
            <button onClick={handleBind} className="btn-ink text-xs">绑定</button>
          </div>
        )}
        {mcMsg && <p className="text-xs text-jade mt-2">{mcMsg}</p>}
      </div>
    </PageLayout>
  )
}
