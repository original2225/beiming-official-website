/**
 * 白名单录入 — 管理员直接把玩家录进白名单（花蝉 2026-11-27 定）
 *
 * 为什么有这个页面：白名单不走邀请码。管理员在群里已经认得这个人，
 * 不该再逼他走「发码 → 注册 → 考 → 申请 → 等审核」那套流程。
 * 这里填完就当场生效，白名单和成员档案一起建好。
 *
 * 与「审核待办」的区别：那边是处理玩家自己提交的申请；
 * 这边是管理员主动录入，不需要考核记录，也不需要玩家先注册官网。
 *
 * 权限：后端 requireAny 只放行 ADMIN / OWNER，普通用户看不到入口。
 */

import { useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { FormField } from '../../components/forms/FormField'
import { directEntryWhitelist } from '../../api/modules/admin'

const SERVER_TYPES = [
  { value: 'SURVIVAL', title: '原版生电服', hint: '考核按生电服服规，游戏账号必填' },
  { value: 'MODPACK', title: '整合包服', hint: '只要求遵守基本友好合作守则' },
]

const PLATFORMS = [
  { value: 'MOJANG', label: '正版（Mojang）' },
  { value: 'LITTLESKIN', label: 'LittleSkin' },
  { value: 'BEIMING', label: '北冥皮肤站' },
  { value: 'OFFLINE', label: '离线服' },
]

export function AdminWhitelistDirectPage() {
  const [serverType, setServerType] = useState('MODPACK')
  const [platform, setPlatform] = useState('OFFLINE')
  const [minecraftName, setMinecraftName] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [qqGroupNickname, setQqGroupNickname] = useState('')
  const [userId, setUserId] = useState('')
  const [reviewComment, setReviewComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [msg, setMsg] = useState('')
  const [ok, setOk] = useState(false)

  const nameMissing = !minecraftName.trim()

  const handleSubmit = async () => {
    if (nameMissing) {
      setMsg('游戏昵称必须填——白名单要能对上具体的人')
      setOk(false)
      return
    }
    setSubmitting(true)
    setMsg('')
    try {
      const created = (await directEntryWhitelist({
        minecraftName: minecraftName.trim(),
        minecraftPlatform: platform,
        serverType,
        displayName: displayName.trim() || undefined,
        qqGroupNickname: qqGroupNickname.trim() || undefined,
        userId: userId.trim() || undefined,
        reviewComment: reviewComment.trim() || undefined,
        reason: '管理员后台直接录入',
        idempotencyKey: `direct-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      })) as { applicationId?: string; status?: string; profileActivation?: { status?: string } } | undefined

      const blocked = created?.status === 'APPROVAL_BLOCKED'
      setOk(!blocked)
      setMsg(
        blocked
          ? `白名单记录已创建（${created?.applicationId}），但成员档案建失败，已转入待处理，请到审核待办跟进`
          : `已录入：${created?.applicationId}（${created?.status}），档案 ${created?.profileActivation?.status ?? '-'}`,
      )
      // 成功后清空玩家信息，方便连着录下一个
      setMinecraftName('')
      setDisplayName('')
      setQqGroupNickname('')
      setUserId('')
      setReviewComment('')
    } catch (e) {
      setOk(false)
      setMsg(e instanceof Error ? e.message : '录入失败')
    }
    setSubmitting(false)
  }

  const hint = SERVER_TYPES.find((t) => t.value === serverType)?.hint ?? ''

  return (
    <PageLayout variant="admin">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="录" />
          <h1 className="font-display text-2xl text-indigo">白名单录入</h1>
        </div>
        <InkStroke />
        <p className="text-xs text-text-muted mt-2">
          直接把玩家录进白名单：不走邀请码、不需要考核。白名单与成员档案同时建好。
        </p>
      </div>

      <div className="panel-ink p-4 space-y-4 max-w-2xl">
        <FormField label="我要进">
          <div className="flex gap-2">
            {SERVER_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                onClick={() => setServerType(t.value)}
                className={`btn-ink text-xs ${serverType === t.value ? '' : 'opacity-50'}`}
              >
                {t.title}
              </button>
            ))}
          </div>
          <p className="text-xs text-text-muted mt-1">{hint}</p>
        </FormField>

        <FormField label="游戏昵称（必填）">
          <input
            className="input-ink w-full"
            value={minecraftName}
            onChange={(e) => setMinecraftName(e.target.value)}
            placeholder="例如 Steve"
          />
        </FormField>

        <FormField label="登录平台">
          <select className="input-ink w-full" value={platform} onChange={(e) => setPlatform(e.target.value)}>
            {PLATFORMS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="站内显示名（可空）">
          <input
            className="input-ink w-full"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="留空则用游戏昵称"
          />
        </FormField>

        <FormField label="QQ 群昵称（可空）">
          <input
            className="input-ink w-full"
            value={qqGroupNickname}
            onChange={(e) => setQqGroupNickname(e.target.value)}
            placeholder="方便日后核对是谁"
          />
        </FormField>

        <FormField label="关联站内用户 ID（可空）">
          <input
            className="input-ink w-full"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="玩家还没注册官网就留空，之后再关联"
          />
        </FormField>

        <FormField label="备注（可空）">
          <input
            className="input-ink w-full"
            value={reviewComment}
            onChange={(e) => setReviewComment(e.target.value)}
            placeholder="例如：老玩家，管理确认过"
          />
        </FormField>

        <div className="flex items-center gap-3">
          <button type="button" className="btn-ink" disabled={submitting} onClick={handleSubmit}>
            {submitting ? '录入中…' : '确认录入'}
          </button>
          {msg && <span className={`text-xs ${ok ? 'text-jade' : 'text-cinnabar'}`}>{msg}</span>}
        </div>
      </div>
    </PageLayout>
  )
}
