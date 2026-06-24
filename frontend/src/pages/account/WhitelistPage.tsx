/**
 * 白名单申请页 — 查看申请状态、提交新申请
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { FormField } from '../../components/forms/FormField'
import { useRequest } from '../../hooks/useRequest'
import { createWhitelistApplication, getMyWhitelistApplications } from '../../api/modules/account'

export function WhitelistPage() {
  const { data: applications, loading: listLoading, run } = useRequest<any>()
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => { run(() => getMyWhitelistApplications()) }, [run])

  const handleCreate = async () => {
    setSubmitting(true)
    try { await createWhitelistApplication({ reason }); setMsg('申请已提交'); run(() => getMyWhitelistApplications()) } catch { setMsg('提交失败') }
    setSubmitting(false)
  }

  return (
    <PageLayout variant="account">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">白名单申请</h1>

      <div className="panel-mc p-4 mb-6">
        <h2 className="font-minecraft text-sm text-mc-gold mb-3">新建申请</h2>
        <div className="flex items-end gap-3">
          <FormField label="申请理由">
            <input type="text" value={reason} onChange={(e) => setReason(e.target.value)} className="bg-surface-dark border border-mc-stone text-text-primary px-3 py-1 text-sm outline-none focus:border-mc-grass w-64" />
          </FormField>
          <button onClick={handleCreate} disabled={submitting} className="btn-mc text-xs">{submitting ? '提交中...' : '提交申请'}</button>
        </div>
        {msg && <p className="text-xs text-mc-emerald mt-2">{msg}</p>}
      </div>

      {listLoading && <LoadingState />}
      {!listLoading && !applications?.items?.length && <EmptyState text="暂无申请记录" />}
      {applications?.items?.map((a: any) => (
        <div key={a.applicationId} className="panel-mc p-3 mb-2 flex items-center justify-between">
          <div>
            <p className="text-sm text-text-primary">申请 #{a.applicationId?.slice(0, 8)}</p>
            <p className="text-xs text-text-muted"><TimeDisplay iso={a.createdAt} /></p>
          </div>
          <StatusBadge status={a.status ?? 'DRAFT'} />
        </div>
      ))}
    </PageLayout>
  )
}
