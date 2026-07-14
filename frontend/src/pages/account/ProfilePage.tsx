/**
 * 个人档案页 — 展示当前用户信息和 Minecraft 绑定状态
 */

import { useEffect } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { DetailPanel } from '../../components/data-display/DetailPanel'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { getMyProfile } from '../../api/modules/account'
import { useCurrentUser } from '../../hooks/useCurrentUser'
import type { ProfileView } from '../../types/view-models'

export function ProfilePage() {
  const user = useCurrentUser()
  const { data, loading, run } = useRequest<ProfileView>()

  useEffect(() => { run(() => getMyProfile()) }, [run])

  return (
    <PageLayout variant="account">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="我" />
          <h1 className="font-display text-2xl text-indigo">个人档案</h1>
        </div>
        <InkStroke />
      </div>
      {loading && <LoadingState />}
      {user && (
        <DetailPanel fields={[
          { label: '用户名', value: <span className="font-display">{user.username}</span> },
          { label: '角色', value: user.role },
          { label: 'Minecraft', value: user.minecraftName ? <span className="text-indigo">{user.minecraftName}</span> : <span className="text-text-muted">未绑定</span> },
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
