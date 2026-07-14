/**
 * 后台概览 — 指标看板 + 待办入口
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { getAdminOverview, getAdminMetrics } from '../../api/modules/admin'
import type { AdminOverviewView, MetricsView } from '../../types/view-models'

export function AdminOverviewPage() {
  const { data: overview, loading, run } = useRequest<AdminOverviewView>()
  const { data: metrics, run: runMetrics } = useRequest<MetricsView>()

  useEffect(() => { run(() => getAdminOverview()) }, [run])
  useEffect(() => { runMetrics(() => getAdminMetrics()) }, [runMetrics])

  return (
    <PageLayout variant="admin">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="管" />
          <h1 className="font-display text-2xl text-indigo">后台概览</h1>
        </div>
        <InkStroke />
      </div>
      {loading && <LoadingState />}

      {metrics && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          {Object.entries(metrics).map(([k, v]) => (
            <div key={k} className="panel-ink p-4 text-center">
              <p className="font-display text-2xl text-ochre">{String(v)}</p>
              <p className="text-xs text-text-muted mt-1">{k}</p>
            </div>
          ))}
        </div>
      )}

      {overview && (
        <DetailPanel fields={[
          { label: '待审核', value: overview.pendingApprovals ?? '—' },
          { label: '活跃用户', value: overview.activeUsers ?? '—' },
          { label: '系统版本', value: overview.version ?? '—' },
        ]} />
      )}
    </PageLayout>
  )
}
