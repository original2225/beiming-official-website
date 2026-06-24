/**
 * 后台概览 — 指标看板 + 待办入口
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
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
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">后台概览</h1>
      {loading && <LoadingState />}

      {metrics && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          {Object.entries(metrics).map(([k, v]) => (
            <div key={k} className="panel-mc p-4 text-center">
              <p className="font-minecraft text-2xl text-mc-gold">{String(v)}</p>
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
