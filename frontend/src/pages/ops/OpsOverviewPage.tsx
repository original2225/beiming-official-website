/**
 * 运维总览 — 节点/任务/审批汇总 + 模块运行态 + 最近审计
 */

import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { getOpsOverview, getOpsSummary } from '../../api/modules/ops'
import type { OpsOverview, OpsSummary, OpsAuditLog } from '../../api/modules/ops'
import { ROUTES } from '../../constants/routes'

/** 运维运行态徽标 — 后端返回状态字符串，样式由前缀映射 */
function OpsBadge({ value }: { value: string }) {
  const tone =
    value === 'ONLINE' || value === 'POSTGRESQL_PRIMARY'
      ? 'border-jade text-jade'
      : value === 'DEGRADED' || value === 'IN_MEMORY'
        ? 'border-ochre text-ochre'
        : 'border-ink-600 text-text-muted'
  return <span className={`inline-block px-2 py-0.5 text-xs font-display border rounded-sm ${tone}`}>{value}</span>
}

function Metric({ label, value, hint }: { label: string; value: number | string; hint?: string }) {
  return (
    <div className="panel-ink p-4">
      <p className="font-display text-2xl text-ochre">{value}</p>
      <p className="text-xs text-text-muted mt-1">{label}</p>
      {hint && <p className="text-xs text-text-muted/70 mt-1">{hint}</p>}
    </div>
  )
}

export function OpsOverviewPage() {
  const { data: overview, loading, error, run } = useRequest<OpsOverview>()
  const { data: summary, run: runSummary } = useRequest<OpsSummary>()

  useEffect(() => {
    run(() => getOpsOverview())
    runSummary(() => getOpsSummary())
  }, [run, runSummary])

  const reload = () => {
    run(() => getOpsOverview())
    runSummary(() => getOpsSummary())
  }

  return (
    <PageLayout variant="ops">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="运" />
          <h1 className="font-display text-2xl text-indigo">运维总览</h1>
        </div>
        <InkStroke />
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error.message} onRetry={reload} />}

      {overview && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
            <Metric label="节点总数" value={overview.nodesTotal} />
            <Metric label="在线节点" value={overview.onlineNodesTotal} />
            <Metric label="离线/禁用" value={overview.offlineNodesTotal} />
            <Metric label="资产总数" value={overview.assetsTotal} />
            <Metric label="待审批" value={overview.pendingApprovalsTotal} hint="高风险任务" />
            <Metric label="运行中任务" value={overview.runningTasksTotal} />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <section className="panel-ink p-4">
              <h2 className="font-display text-sm text-indigo mb-3">模块运行态</h2>
              {summary ? (
                <dl className="grid grid-cols-2 gap-y-2 text-sm">
                  <dt className="text-text-muted">服务</dt>
                  <dd className="text-text-secondary">{summary.service}</dd>
                  <dt className="text-text-muted">端口</dt>
                  <dd className="text-text-secondary">{summary.port}</dd>
                  <dt className="text-text-muted">存储模式</dt>
                  <dd><OpsBadge value={summary.storageMode} /></dd>
                  <dt className="text-text-muted">认证适配</dt>
                  <dd><OpsBadge value={summary.authMode} /></dd>
                  <dt className="text-text-muted">节点适配</dt>
                  <dd><OpsBadge value={summary.nodeAdapterMode} /></dd>
                  <dt className="text-text-muted">外部执行器</dt>
                  <dd className="text-text-secondary">
                    {summary.externalExecutorConnected ? '已连接' : '未连接'}
                  </dd>
                  <dt className="text-text-muted">最近心跳</dt>
                  <dd>{summary.lastHeartbeatAt ? <TimeDisplay iso={summary.lastHeartbeatAt} /> : '—'}</dd>
                </dl>
              ) : (
                <EmptyState text="运行态未加载" />
              )}
              {summary && summary.productionGaps.length > 0 && (
                <div className="mt-4 pt-3 border-t border-ink-600/30">
                  <p className="text-xs text-text-muted mb-2">未补齐项（后端如实上报）</p>
                  <ul className="flex flex-wrap gap-2">
                    {summary.productionGaps.map((gap) => (
                      <li key={gap} className="text-xs px-2 py-0.5 border border-ochre/60 text-ochre rounded-sm">
                        {gap}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </section>

            <section className="panel-ink p-4">
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-display text-sm text-indigo">最近审计</h2>
                <Link to={ROUTES.OPS_ASSETS} className="text-xs text-text-muted hover:text-ochre">
                  查看资产 →
                </Link>
              </div>
              {overview.recentAudits.length > 0 ? (
                <ul className="space-y-2">
                  {overview.recentAudits.map((audit: OpsAuditLog) => (
                    <li key={audit.id} className="text-sm flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="text-text-secondary truncate">{audit.action}</p>
                        <p className="text-xs text-text-muted truncate">
                          {audit.actorUserId} · {audit.targetType}/{audit.targetId}
                        </p>
                      </div>
                      <TimeDisplay iso={audit.createdAt} />
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState text="暂无审计记录" />
              )}
            </section>
          </div>
        </>
      )}
    </PageLayout>
  )
}