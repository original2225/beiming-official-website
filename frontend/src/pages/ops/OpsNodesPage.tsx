/**
 * 节点管理 — 节点列表、状态筛选、指标查看
 */

import { useCallback, useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { DataTable } from '../../components/data-display/DataTable'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getOpsNodes, getOpsNodeMetrics } from '../../api/modules/ops'
import type { OpsNode, OpsNodeMetric, PageResult } from '../../api/modules/ops'

const NODE_STATUSES = ['', 'PENDING_REGISTRATION', 'ONLINE', 'OFFLINE', 'DISABLED', 'REVOKED']

function statusTone(status: string) {
  if (status === 'ONLINE') return 'border-jade text-jade'
  if (status === 'PENDING_REGISTRATION') return 'border-ochre text-ochre'
  if (status === 'OFFLINE') return 'border-ink-600 text-text-muted'
  return 'border-cinnabar text-cinnabar'
}

function NodeStatus({ status }: { status: string }) {
  return (
    <span className={`inline-block px-2 py-0.5 text-xs font-display border rounded-sm ${statusTone(status)}`}>
      {status}
    </span>
  )
}

export function OpsNodesPage() {
  const [status, setStatus] = useState('')
  const [keyword, setKeyword] = useState('')
  const { page, pageSize, goTo, reset } = usePagination()
  const { data, loading, error, run } = useRequest<PageResult<OpsNode>>()
  const [selected, setSelected] = useState<string | null>(null)
  const { data: metric, run: runMetric } = useRequest<OpsNodeMetric>()

  const load = useCallback(() => {
    run(() =>
      getOpsNodes({
        page,
        pageSize,
        status: status || undefined,
        keyword: keyword || undefined,
        sort: 'updatedAt_desc',
      }),
    )
  }, [run, page, pageSize, status, keyword])

  useEffect(() => {
    load()
  }, [load])

  const openMetrics = (nodeId: string) => {
    setSelected(nodeId)
    runMetric(() => getOpsNodeMetrics(nodeId))
  }

  return (
    <PageLayout variant="ops">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="节" />
          <h1 className="font-display text-2xl text-indigo">节点管理</h1>
        </div>
        <InkStroke />
      </div>

      <div className="flex flex-wrap items-center gap-2 mb-4">
        <input
          value={keyword}
          onChange={(e) => {
            setKeyword(e.target.value)
            reset()
          }}
          placeholder="搜索节点名称或 ID"
          className="px-3 py-1.5 text-sm bg-surface border border-ink-600/40 rounded-md text-text-primary placeholder:text-text-muted focus:outline-none focus:border-indigo"
        />
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value)
            reset()
          }}
          className="px-3 py-1.5 text-sm bg-surface border border-ink-600/40 rounded-md text-text-primary focus:outline-none focus:border-indigo"
        >
          {NODE_STATUSES.map((value) => (
            <option key={value || 'ALL'} value={value}>
              {value || '全部状态'}
            </option>
          ))}
        </select>
        <button onClick={load} className="btn-ink text-sm px-4 py-1.5">
          刷新
        </button>
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error.message} onRetry={load} />}

      {data && data.items.length === 0 && <EmptyState title="暂无节点" text="还没有注册任何运维节点。" />}

      {data && data.items.length > 0 && (
        <>
          <DataTable<OpsNode>
            rowKey={(row) => row.nodeId}
            data={data.items}
            columns={[
              {
                key: 'node',
                header: '节点',
                render: (row) => (
                  <div>
                    <p className="text-text-primary">{row.displayName}</p>
                    <p className="text-xs text-text-muted">{row.nodeId}</p>
                  </div>
                ),
              },
              { key: 'status', header: '状态', render: (row) => <NodeStatus status={row.status} /> },
              {
                key: 'endpoint',
                header: '接入地址',
                render: (row) => <span className="text-xs text-text-muted">{row.endpointSummary}</span>,
              },
              {
                key: 'version',
                header: '版本',
                render: (row) => <span className="text-xs">{row.version ?? '—'}</span>,
              },
              {
                key: 'heartbeat',
                header: '最近心跳',
                render: (row) =>
                  row.lastHeartbeatAt ? <TimeDisplay iso={row.lastHeartbeatAt} /> : <span className="text-xs text-text-muted">—</span>,
              },
              {
                key: 'ops',
                header: '操作',
                render: (row) => (
                  <button onClick={() => openMetrics(row.nodeId)} className="btn-ink-ghost text-xs px-2 py-0.5">
                    指标
                  </button>
                ),
              },
            ]}
          />
          <Pagination page={data.page} pageSize={data.pageSize} total={data.total} onChange={goTo} />
        </>
      )}

      {selected && (
        <section className="panel-ink p-4 mt-4">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-display text-sm text-indigo">节点指标 · {selected}</h2>
            <button onClick={() => setSelected(null)} className="text-xs text-text-muted hover:text-ochre">
              关闭
            </button>
          </div>
          {metric && metric.cpuUsagePercent !== undefined ? (
            <div className="grid grid-cols-3 gap-3">
              {[
                { label: 'CPU', value: metric.cpuUsagePercent },
                { label: '内存', value: metric.memoryUsagePercent },
                { label: '磁盘', value: metric.diskUsagePercent },
              ].map((item) => (
                <div key={item.label} className="text-center">
                  <p className="font-display text-xl text-ochre">
                    {(item.value ?? 0).toFixed(1)}%
                  </p>
                  <p className="text-xs text-text-muted mt-1">{item.label}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState text="该节点暂时没有指标快照，等待节点心跳上报。" />
          )}
        </section>
      )}
    </PageLayout>
  )
}