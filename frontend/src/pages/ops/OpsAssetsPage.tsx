/**
 * 资产管理 — 节点下的资产清单与归属模块
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
import { getOpsAssets } from '../../api/modules/ops'
import type { OpsAsset, PageResult } from '../../api/modules/ops'

const ASSET_TYPES = ['', 'MINECRAFT_INSTANCE', 'DOCKER_CONTAINER', 'VM', 'FILE_ROOT']

export function OpsAssetsPage() {
  const [assetType, setAssetType] = useState('')
  const [keyword, setKeyword] = useState('')
  const { page, pageSize, goTo, reset } = usePagination()
  const { data, loading, error, run } = useRequest<PageResult<OpsAsset>>()

  const load = useCallback(() => {
    run(() =>
      getOpsAssets({
        page,
        pageSize,
        assetType: assetType || undefined,
        keyword: keyword || undefined,
      }),
    )
  }, [run, page, pageSize, assetType, keyword])

  useEffect(() => {
    load()
  }, [load])

  return (
    <PageLayout variant="ops">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="资" />
          <h1 className="font-display text-2xl text-indigo">资产管理</h1>
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
          placeholder="搜索资产名称或 ID"
          className="px-3 py-1.5 text-sm bg-surface border border-ink-600/40 rounded-md text-text-primary placeholder:text-text-muted focus:outline-none focus:border-indigo"
        />
        <select
          value={assetType}
          onChange={(e) => {
            setAssetType(e.target.value)
            reset()
          }}
          className="px-3 py-1.5 text-sm bg-surface border border-ink-600/40 rounded-md text-text-primary focus:outline-none focus:border-indigo"
        >
          {ASSET_TYPES.map((value) => (
            <option key={value || 'ALL'} value={value}>
              {value || '全部类型'}
            </option>
          ))}
        </select>
        <button onClick={load} className="btn-ink text-sm px-4 py-1.5">
          刷新
        </button>
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error.message} onRetry={load} />}

      {data && data.items.length === 0 && <EmptyState title="暂无资产" text="节点尚未上报任何资产快照。" />}

      {data && data.items.length > 0 && (
        <>
          <DataTable<OpsAsset>
            rowKey={(row) => row.assetId}
            data={data.items}
            columns={[
              {
                key: 'asset',
                header: '资产',
                render: (row) => (
                  <div>
                    <p className="text-text-primary">{row.displayName}</p>
                    <p className="text-xs text-text-muted">{row.assetId}</p>
                  </div>
                ),
              },
              {
                key: 'type',
                header: '类型',
                render: (row) => <span className="text-xs text-text-muted">{row.assetType}</span>,
              },
              {
                key: 'status',
                header: '状态',
                render: (row) => <span className="text-xs text-text-secondary">{row.status}</span>,
              },
              {
                key: 'owner',
                header: '归属模块',
                render: (row) => <span className="text-xs text-text-muted">{row.ownerModule ?? '—'}</span>,
              },
              {
                key: 'node',
                header: '所属节点',
                render: (row) => <span className="text-xs text-text-muted">{row.nodeId}</span>,
              },
              {
                key: 'updated',
                header: '更新时间',
                render: (row) => <TimeDisplay iso={row.updatedAt} />,
              },
            ]}
          />
          <Pagination page={data.page} pageSize={data.pageSize} total={data.total} onChange={goTo} />
        </>
      )}
    </PageLayout>
  )
}