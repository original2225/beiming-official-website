/**
 * 个人档案页 — 展示当前用户信息和 Minecraft 绑定状态
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { useRequest } from '../../hooks/useRequest'
import { getMyProfile } from '../../api/modules/account'
import { useCurrentUser } from '../../hooks/useCurrentUser'

export function ProfilePage() {
  const user = useCurrentUser()
  const { data, loading, run } = useRequest<any>()

  useEffect(() => { run(() => getMyProfile()) }, [run])

  return (
    <PageLayout variant="account">
      <h1 className="font-minecraft text-2xl text-mc-grass mb-6">个人档案</h1>
      {loading && <LoadingState />}
      {user && (
        <DetailPanel fields={[
          { label: '用户名', value: <span className="font-minecraft">{user.username}</span> },
          { label: '角色', value: user.role },
          { label: 'Minecraft', value: user.minecraftName ? <span className="text-mc-grass">{user.minecraftName}</span> : <span className="text-text-muted">未绑定</span> },
        ]} />
      )}
      {data && (
        <div className="mt-4">
          <DetailPanel fields={[
            { label: '成员组', value: data.groupName ?? '—' },
            { label: '事迹', value: data.milestones?.length ? `${data.milestones.length} 项` : '暂无' },
          ]} />
        </div>
      )}
    </PageLayout>
  )
}
