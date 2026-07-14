/**
 * 入服流程页 — 显示进度、当前步骤、下一步操作
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { getOnboardingProgress, getNextAction } from '../../api/modules/account'
import type { NextActionView, OnboardingProgressView } from '../../types/view-models'

export function OnboardingPage() {
  const { data, loading, run } = useRequest<OnboardingProgressView>()
  const { data: action, run: runAction } = useRequest<NextActionView>()

  useEffect(() => { run(() => getOnboardingProgress()) }, [run])
  useEffect(() => { runAction(() => getNextAction()) }, [runAction])

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
      {action && (
        <div className="panel-ink p-4 mt-3 border-ochre">
          <p className="font-display text-sm text-ochre mb-2">下一步</p>
          <p className="text-sm text-text-primary">{action.description ?? action.action ?? '等待服务器响应'}</p>
        </div>
      )}
    </PageLayout>
  )
}
