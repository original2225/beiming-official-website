/**
 * 内容管理 — 后台内容列表
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { SearchFilter } from '../../components/forms/SearchFilter'
import { DataTable } from '../../components/data-display/DataTable'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getAdminContent } from '../../api/modules/admin'

export function AdminContentPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<any>()
  const [query, setQuery] = useState('')

  useEffect(() => { run(() => getAdminContent({ page, pageSize, ...(query ? { query } : {}) })) }, [run, page, pageSize, query])

  return (
    <PageLayout variant="admin">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">内容管理</h1>
      <div className="mb-4">
        <SearchFilter onSearch={setQuery} placeholder="搜索内容..." />
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items?.length && <EmptyState text="暂无内容" />}
      {data?.items && (
        <DataTable
          columns={[
            { key: 'title', header: '标题', render: (c: any) => <span className="text-sm">{c.title}</span> },
            { key: 'status', header: '状态', render: (c: any) => <StatusBadge status={c.status ?? 'DRAFT'} /> },
            { key: 'category', header: '分类', render: (c: any) => <span className="text-xs text-text-muted">{c.category ?? '—'}</span> },
            { key: 'updatedAt', header: '更新时间', render: (c: any) => <TimeDisplay iso={c.updatedAt ?? c.createdAt} /> },
          ]}
          data={data.items}
          rowKey={(c) => c.contentId}
        />
      )}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
