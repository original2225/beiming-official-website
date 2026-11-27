/**
 * 白名单申请页 — 选择服型、绑定游戏账号、提交申请
 *
 * 流程（花蝉 2026-11-27 定）：考核通过后才来这里申请邀请码。
 * 绑定的游戏身份在这一步落地，因为这一刻有人工审核，能真正核对归属；
 * 注册时填个昵称只证明「它存在」，不证明「是我的」。
 */

import { useEffect, useMemo, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { FormField } from '../../components/forms/FormField'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { createWhitelistApplication, getMyWhitelistApplications } from '../../api/modules/account'
import type { WhitelistApplicationPage, WhitelistApplicationView } from '../../types/view-models'

/** 服型：决定考核内容与绑定是否强制。 */
type ServerType = 'SURVIVAL' | 'MODPACK'

/**
 * 平台下拉。顺序就是业务优先级（正版 > LittleSkin > 北冥皮肤站），
 * 后端 MinecraftIdentityResolver.Platform 的枚举顺序与此一致，改一处要改两处。
 */
const PLATFORMS = [
  { value: 'MOJANG', label: '正版（Microsoft）' },
  { value: 'LITTLESKIN', label: 'LittleSkin' },
  { value: 'BEIMING', label: '北冥皮肤站' },
] as const

const SERVER_TYPES: { value: ServerType; title: string; hint: string }[] = [
  {
    value: 'MODPACK',
    title: '整合包服',
    hint: '遵守基本友好合作守则即可，游戏账号建议填',
  },
  {
    value: 'SURVIVAL',
    title: '原版生电服',
    hint: '需通过生电服服规考核，游戏账号必填',
  },
]

export function WhitelistPage() {
  const { data: applications, loading: listLoading, run } = useRequest<WhitelistApplicationPage>()
  const [serverType, setServerType] = useState<ServerType>('MODPACK')
  const [platform, setPlatform] = useState<string>('MOJANG')
  const [mcName, setMcName] = useState('')
  const [qqNick, setQqNick] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => { run(() => getMyWhitelistApplications()) }, [run])

  const isSurvival = serverType === 'SURVIVAL'
  // 生电服必须填游戏昵称；整合包可以不填
  const nameMissing = isSurvival && !mcName.trim()

  const handleCreate = async () => {
    if (nameMissing) { setMsg('生电服需要填写游戏昵称'); return }
    setSubmitting(true)
    try {
      const created = await createWhitelistApplication({
        serverType,
        minecraftPlatform: platform,
        minecraftName: mcName.trim() || undefined,
        qqGroupNickname: qqNick.trim() || undefined,
      }) as { status?: string } | undefined
      // B 方案：管理员邀请码 + 考核通过会当场给白名单，不再是「提交后等人审」。
      // 这里必须按返回状态说清楚，否则用户会以为还要排队等。
      setMsg(created?.status === 'APPROVED'
        ? '考核已通过，白名单已发放，可以直接进服了'
        : '申请已提交，等待管理员审核')
      run(() => getMyWhitelistApplications())
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '提交失败')
    }
    setSubmitting(false)
  }

  const hint = useMemo(() => SERVER_TYPES.find((t) => t.value === serverType)?.hint ?? '', [serverType])

  return (
    <PageLayout variant="account">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="白" />
          <h1 className="font-display text-2xl text-indigo">白名单申请</h1>
        </div>
        <InkStroke />
      </div>

      <div className="panel-ink p-4 mb-6 space-y-4">
        <h2 className="font-display text-sm text-ochre">新建申请</h2>

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

        <div className="flex items-end gap-3">
          <FormField label="登录平台">
            <select
              value={platform}
              onChange={(e) => setPlatform(e.target.value)}
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
            >
              {PLATFORMS.map((p) => (
                <option key={p.value} value={p.value}>{p.label}</option>
              ))}
            </select>
          </FormField>
          <FormField label={isSurvival ? '游戏昵称（必填）' : '游戏昵称（建议填）'}>
            <input
              type="text"
              value={mcName}
              onChange={(e) => setMcName(e.target.value)}
              placeholder="3-16 位字母数字下划线"
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo w-56"
            />
          </FormField>
          <FormField label="QQ 群昵称">
            <input
              type="text"
              value={qqNick}
              onChange={(e) => setQqNick(e.target.value)}
              placeholder="方便管理员核对"
              className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo w-40"
            />
          </FormField>
          <button onClick={handleCreate} disabled={submitting} className="btn-ink text-xs">
            {submitting ? '提交中...' : '提交申请'}
          </button>
        </div>

        {msg && <p className="text-xs text-jade">{msg}</p>}
      </div>

      {listLoading && <LoadingState />}
      {!listLoading && !applications?.items?.length && <EmptyState text="暂无申请记录" />}
      {applications?.items?.map((a: WhitelistApplicationView) => (
        <div key={a.applicationId} className="panel-ink p-3 mb-2 flex items-center justify-between">
          <div>
            <p className="text-sm text-text-primary">申请 #{a.applicationId?.slice(0, 8)}</p>
            <p className="text-xs text-text-muted"><TimeDisplay iso={a.createdAt} /></p>
          </div>
          <StatusBadge status={a.status ?? 'DRAFT'} />
        </div>
      ))}
    </PageLayout>
  )
}