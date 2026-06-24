/**
 * 考勤积分页 — 积分账户、流水、排名
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { useRequest } from '../../hooks/useRequest'
import { getMyAttendanceAccount, getMyRanking } from '../../api/modules/account'
import type { AttendanceAccountView, RankingView } from '../../types/view-models'

export function AttendancePage() {
  const { data: account, loading, run } = useRequest<AttendanceAccountView>()
  const { data: ranking, run: runRank } = useRequest<RankingView>()

  useEffect(() => { run(() => getMyAttendanceAccount()) }, [run])
  useEffect(() => { runRank(() => getMyRanking()) }, [runRank])

  return (
    <PageLayout variant="account">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">考勤积分</h1>
      {loading && <LoadingState />}
      {account && (
        <DetailPanel fields={[
          { label: '总积分', value: <span className="font-minecraft text-mc-gold text-lg">{account.totalPoints ?? account.points ?? 0}</span> },
          { label: '当月积分', value: account.monthlyPoints ?? '—' },
          { label: '活跃度', value: account.activityLevel ?? '—' },
        ]} />
      )}
      {ranking && (
        <div className="mt-4 panel-mc p-4">
          <h2 className="font-minecraft text-sm text-mc-gold mb-2">我的排名</h2>
          <p className="text-sm text-text-primary">当前排名: <span className="font-minecraft text-mc-grass">#{ranking.rank ?? ranking.position ?? '—'}</span></p>
        </div>
      )}
    </PageLayout>
  )
}
