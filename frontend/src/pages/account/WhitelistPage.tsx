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
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { createWhitelistApplication, getMyWhitelistApplications } from '../../api/modules/account'
import type { WhitelistApplicationPage, WhitelistApplicationView } from '../../types/view-models'

export function WhitelistPage() {
  const { data: applications, loading: listLoading, run } = useRequest<WhitelistApplicationPage>()
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
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="白" />
          <h1 className="font-display text-2xl text-indigo">白名单申请</h1>
        </div>
        <InkStroke />
      </div>

      <div className="panel-ink p-4 mb-6">
        <h2 className="font-display text-sm text-ochre mb-3">新建申请</h2>
        <div className="flex items-end gap-3">
          <FormField label="申请理由">
            <input type="text" value={reason} onChange={(e) => setReason(e.target.value)} className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo w-64" />
          </FormField>
          <button onClick={handleCreate} disabled={submitting} className="btn-ink text-xs">{submitting ? '提交中...' : '提交申请'}</button>
        </div>
        {msg && <p className="text-xs text-jade mt-2">{msg}</p>}
      </div>

      {listLoading && <LoadingState />}
      {!listLoading && !applications?.items?.length && <EmptyState text="暂无申请记录" />}
      {applications?.items?.map((a: WhitelistApplicationView) => (
        <div key={a.applicationId} className="panel-ink p-3 mb-2 flex items-center justify-between">
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
