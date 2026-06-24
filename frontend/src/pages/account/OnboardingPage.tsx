/**
 * 入服流程页 — 显示进度、当前步骤、下一步操作
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
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
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">入服流程</h1>
      {loading && <LoadingState />}
      {data && (
        <div className="panel-mc p-4">
          <p className="text-sm text-text-primary mb-1">当前进度: {data.currentStep ?? '未开始'}</p>
          <p className="text-xs text-text-muted">状态: {data.status ?? '—'}</p>
        </div>
      )}
      {action && (
        <div className="panel-mc p-4 mt-3 border-mc-gold">
          <p className="font-minecraft text-sm text-mc-gold mb-2">下一步</p>
          <p className="text-sm text-text-primary">{action.description ?? action.action ?? '等待服务器响应'}</p>
        </div>
      )}
    </PageLayout>
  )
}
