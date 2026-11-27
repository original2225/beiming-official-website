/**
 * 入服流程页 — 显示进度、当前步骤，并在这一步选方向与服型
 *
 * 服型为什么在这里选（花蝉 2026-11-27 定）：考核内容按服型分岔，
 * 而考核在申请邀请码之前，所以服型必须在考核前定下来。
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { FormField } from '../../components/forms/FormField'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { getOnboardingProgress, getNextAction, chooseDirection } from '../../api/modules/account'
import type { NextActionView, OnboardingProgressView } from '../../types/view-models'

type ServerType = 'SURVIVAL' | 'MODPACK'

const DIRECTIONS = [
  { value: 'GENERAL', label: '通用（生存、合作向）' },
  { value: 'REDSTONE', label: '红石' },
  { value: 'BUILDING', label: '建筑' },
  { value: 'LATE_GAME', label: '后期（全物品、伪和平等）' },
] as const

const SERVER_TYPES: { value: ServerType; title: string; hint: string }[] = [
  { value: 'MODPACK', title: '整合包服', hint: '只需遵守基本友好合作守则，题目较少' },
  { value: 'SURVIVAL', title: '原版生电服', hint: '需通过生电服服规考核，申请时游戏账号必填' },
]

export function OnboardingPage() {
  const { data, loading, run } = useRequest<OnboardingProgressView>()
  const { data: action, run: runAction } = useRequest<NextActionView>()
  const [direction, setDirection] = useState<string>('GENERAL')
  const [serverType, setServerType] = useState<ServerType>('MODPACK')
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => { run(() => getOnboardingProgress()) }, [run])
  useEffect(() => { runAction(() => getNextAction()) }, [runAction])

  const handleChoose = async () => {
    setSaving(true)
    try {
      // 方向和服型一起提交：分开提交会让人先考完通用卷才发现服型选错了
      await chooseDirection({ reviewDirection: direction, serverType })
      setMsg('已保存')
      run(() => getOnboardingProgress())
      runAction(() => getNextAction())
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '保存失败')
    }
    setSaving(false)
  }

  const hint = SERVER_TYPES.find((t) => t.value === serverType)?.hint ?? ''

  return (
    <PageLayout variant="account">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="待" />
          <h1 className="font-display text-2xl text-indigo">入服流程</h1>
        </div>
        <InkStroke />
      </div>
      {loading && <LoadingState />}
      {data && (
        <div className="panel-ink p-4">
          <p className="text-sm text-text-primary mb-1">当前进度: {data.currentStep ?? '未开始'}</p>
          <p className="text-xs text-text-muted">状态: {data.status ?? '—'}</p>
        </div>
      )}
      <div className="panel-ink p-4 mt-3 space-y-3">
        <h2 className="font-display text-sm text-ochre">选择服型与方向</h2>

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

        <FormField label="游玩方向">
          <select
            value={direction}
            onChange={(e) => setDirection(e.target.value)}
            className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
          >
            {DIRECTIONS.map((d) => (
              <option key={d.value} value={d.value}>{d.label}</option>
            ))}
          </select>
        </FormField>

        <button onClick={handleChoose} disabled={saving} className="btn-ink text-xs">
          {saving ? '保存中...' : '保存选择'}
        </button>
        {msg && <p className="text-xs text-jade">{msg}</p>}
      </div>

      {action && (
        <div className="panel-ink p-4 mt-3 border-ochre">
          <p className="font-display text-sm text-ochre mb-2">下一步</p>
          <p className="text-sm text-text-primary">{action.description ?? action.action ?? '等待服务器响应'}</p>
        </div>
      )}
    </PageLayout>
  )
}
