/** 服务器状态详情页 */
import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { DataTable } from '../../components/data-display/DataTable'
import { useRequest } from '../../hooks/useRequest'
import { getServerOverview, getServerLines } from '../../api/modules/public'
import type { ServerStatusSummary, LineSummary } from '../../types/domain'

export function ServerStatusPage() {
  const { data, loading, run: runStatus } = useRequest<ServerStatusSummary>()
  const { data: lines, run: runLines } = useRequest<LineSummary[]>()

  useEffect(() => { runStatus(() => getServerOverview()) }, [runStatus])
  useEffect(() => { runLines(() => getServerLines()) }, [runLines])

  return (
    <PageLayout variant="public">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">服务器状态</h1>
      {loading && <LoadingState />}
      {data && (
        <DetailPanel fields={[
          { label: '状态', value: <span className={`font-minecraft ${data.online ? 'text-mc-grass' : 'text-mc-redstone'}`}>{data.online ? '在线' : '离线'}</span> },
          { label: '玩家', value: data.online ? `${data.playerCount}/${data.maxPlayers}` : '—' },
          { label: '版本', value: data.version },
          { label: 'MOTD', value: data.motd },
        ]} />
      )}
      {lines && lines.length > 0 && (
        <div className="mt-6">
          <h2 className="font-minecraft text-lg text-mc-gold mb-3">线路</h2>
          <DataTable
            columns={[
              { key: 'name', header: '名称', render: (l: LineSummary) => l.name },
              { key: 'address', header: '地址', render: (l: LineSummary) => l.address },
              { key: 'latency', header: '延迟', render: (l: LineSummary) => `${l.latency}ms` },
              { key: 'status', header: '状态', render: (l: LineSummary) => <span className={l.online ? 'text-mc-grass' : 'text-mc-redstone'}>{l.online ? '在线' : '离线'}</span> },
            ]}
            data={lines}
            rowKey={(l) => l.lineId}
          />
        </div>
      )}
    </PageLayout>
  )
}
