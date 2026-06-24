/**
 * 用户管理 — 后台用户列表 + 角色修改
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { SearchFilter } from '../../components/forms/SearchFilter'
import { DataTable } from '../../components/data-display/DataTable'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { Pagination } from '../../components/data-display/Pagination'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getAdminUsers } from '../../api/modules/admin'
import type { AdminUserPage, AdminUserView } from '../../types/view-models'

export function AdminUsersPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<AdminUserPage>()
  const [query, setQuery] = useState('')

  useEffect(() => { run(() => getAdminUsers({ page, pageSize, ...(query ? { query } : {}) })) }, [run, page, pageSize, query])

  return (
    <PageLayout variant="admin">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">用户管理</h1>
      <div className="mb-4">
        <SearchFilter onSearch={setQuery} placeholder="搜索用户名..." />
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items?.length && <EmptyState text="暂无用户" />}
      {data?.items && (
        <DataTable
          columns={[
            { key: 'username', header: '用户名', render: (u: AdminUserView) => <span className="font-minecraft text-sm">{u.username}</span> },
            { key: 'role', header: '角色', render: (u: AdminUserView) => <StatusBadge status={u.status ?? 'ACTIVE'} /> },
            { key: 'createdAt', header: '注册时间', render: (u: AdminUserView) => <span className="text-xs text-text-muted">{u.createdAt ?? '—'}</span> },
          ]}
          data={data.items}
          rowKey={(u) => u.userId}
        />
      )}
      {data && data.total > pageSize && <Pagination page={page} pageSize={pageSize} total={data.total} onChange={goTo} />}
    </PageLayout>
  )
}
