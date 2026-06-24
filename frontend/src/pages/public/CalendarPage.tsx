/** 日历页 */
import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import { getCalendarEvents } from '../../api/modules/public'
import type { CalendarEventSummary } from '../../types/domain'

export function CalendarPage() {
  const { data, loading, run } = useRequest<{ items: CalendarEventSummary[] }>()

  useEffect(() => { run(() => getCalendarEvents({ pageSize: 50 })) }, [run])

  return (
    <PageLayout variant="public">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">日程</h1>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无日程" />}
      {data?.items.map((e) => (
        <div key={e.eventId} className="panel-mc p-3 mb-2 flex items-center justify-between">
          <div>
            <span className="text-sm text-text-primary">{e.title}</span>
            <span className="text-xs text-text-muted ml-2">[{e.type}]</span>
          </div>
          <span className="text-xs text-text-muted"><TimeDisplay iso={e.startTime} /></span>
        </div>
      ))}
    </PageLayout>
  )
}
