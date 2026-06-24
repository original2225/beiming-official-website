/**
 * 考试记录页 — 考试历史列表
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { StatusBadge } from '../../components/data-display/StatusBadge'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { getMyExamSessions } from '../../api/modules/account'

export function ExamPage() {
  const { data, loading, run } = useRequest<any>()

  useEffect(() => { run(() => getMyExamSessions()) }, [run])

  return (
    <PageLayout variant="account">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">考试记录</h1>
      {loading && <LoadingState />}
      {!loading && !data?.items?.length && <EmptyState text="暂无考试记录" />}
      {data?.items?.map((s: any) => (
        <div key={s.sessionId} className="panel-mc p-3 mb-2 flex items-center justify-between">
          <div>
            <p className="text-sm text-text-primary">{s.title ?? s.direction ?? '考试'}</p>
            <p className="text-xs text-text-muted"><TimeDisplay iso={s.createdAt} /></p>
          </div>
          <StatusBadge status={s.status ?? 'DRAFT'} />
        </div>
      ))}
    </PageLayout>
  )
}
