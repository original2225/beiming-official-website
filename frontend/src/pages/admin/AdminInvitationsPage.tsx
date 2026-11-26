/**
 * 邀请码管理 — 后台邀请码列表 + 新建 + 禁用
 *
 * 2026-11-26 花蝉提的：注册失败只说「请稍后重试」时，用户既不知道
 * 是哪一步错了，也不知道去哪拿可用的码。这个页面让管理员能一眼看到
 * 每个码的类型、剩余次数、状态，并把新码发给需要的人。
 *
 * 权限：后端 requireAdmin 只放行 ADMIN / OWNER，普通用户看不到入口。
 */

import { useEffect, useState } from 'react'
import { PageLayout } from '../../components/layout/PageLayout'
import { LoadingState } from '../../components/feedback/LoadingState'
import { EmptyState } from '../../components/feedback/EmptyState'
import { DataTable } from '../../components/data-display/DataTable'
import { Pagination } from '../../components/data-display/Pagination'
import { Seal } from '../../components/common/Seal'
import { InkStroke } from '../../components/common/InkStroke'
import { useRequest } from '../../hooks/useRequest'
import { usePagination } from '../../hooks/usePagination'
import { getAdminInvitations, createInvitation, disableInvitation } from '../../api/modules/admin'
import { ApiError } from '../../types/api'
import { describeApiError } from '../../api/errorMessages'

interface InvitationRow {
  id: string
  codePrefix: string
  type: string
  status: string
  boundRoles: string[]
  boundPermissions: string[]
  maxUses: number
  usedCount: number
  expiresAt: string | null
  createdBy: string
  createdAt: string
  disabledAt: string | null
}

interface InvitationPage {
  items: InvitationRow[]
  total: number
  page: number
  pageSize: number
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: '可用',
  DISABLED: '已禁用',
  EXPIRED: '已过期',
  EXHAUSTED: '已用尽',
}

// StatusBadge 只认 DRAFT/ACTIVE/PUBLISHED/OFFLINE/ARCHIVED/DELETED 六种，
// 邀请码的 EXHAUSTED / EXPIRED 不在其中，所以自己画一个。
const STATUS_STYLE: Record<string, string> = {
  ACTIVE: 'border-jade/40 text-jade',
  EXHAUSTED: 'border-ochre/40 text-ochre',
  EXPIRED: 'border-cinnabar/40 text-cinnabar',
  DISABLED: 'border-cinnabar/40 text-cinnabar',
}

function InvitationStatus({ status }: { status: string }) {
  return (
    <span
      className={`inline-block px-2 py-0.5 text-xs font-display rounded-sm border ${STATUS_STYLE[status] ?? 'border-ink-600 text-text-muted'}`}
    >
      {STATUS_LABEL[status] ?? status}
    </span>
  )
}

export function AdminInvitationsPage() {
  const { page, pageSize, goTo } = usePagination()
  const { data, loading, run } = useRequest<InvitationPage>()
  const [statusFilter, setStatusFilter] = useState('')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  // 新建表单
  const [showCreate, setShowCreate] = useState(false)
  const [newType, setNewType] = useState<'PLAYER' | 'ADMIN'>('PLAYER')
  const [newMaxUses, setNewMaxUses] = useState('10')
  const [newReason, setNewReason] = useState('')
  const [creating, setCreating] = useState(false)

  const reload = () => {
    run(() =>
      getAdminInvitations({
        page,
        pageSize,
        ...(statusFilter ? { status: statusFilter } : {}),
      }) as Promise<InvitationPage>,
    )
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize, statusFilter])

  const rows = data?.items ?? []

  const handleCreate = async () => {
    setErr('')
    setMsg('')
    const maxUses = Number(newMaxUses)
    if (!Number.isInteger(maxUses) || maxUses < 1 || maxUses > 1000) {
      setErr('可用次数需为 1-1000 的整数')
      return
    }
    if (!newReason.trim()) {
      setErr('请填写创建原因（会写进审计日志）')
      return
    }
    setCreating(true)
    try {
      const created = (await createInvitation({
        type: newType,
        boundRoles: newType === 'ADMIN' ? ['ADMIN'] : ['USER'],
        boundPermissions: [],
        maxUses,
        reason: newReason.trim(),
      })) as { codeValue?: string; codePrefix?: string }

      // 明文码只在创建时返回一次，务必提示管理员立刻复制保存
      const plain = created?.codeValue
      setMsg(
        plain
          ? `已生成邀请码：${plain}（只显示这一次，请立即复制保存）`
          : '邀请码已创建',
      )
      setShowCreate(false)
      setNewReason('')
      reload()
    } catch (e) {
      setErr(e instanceof ApiError ? describeApiError(e.code, e.message) : '创建失败，请稍后重试')
    } finally {
      setCreating(false)
    }
  }

  const handleDisable = async (row: InvitationRow) => {
    const reason = window.prompt(`禁用邀请码 ${row.codePrefix}… 的原因（会写进审计日志）：`)
    if (!reason || !reason.trim()) return
    setErr('')
    setMsg('')
    try {
      await disableInvitation(row.id)
      setMsg('邀请码已禁用')
      reload()
    } catch (e) {
      setErr(e instanceof ApiError ? describeApiError(e.code, e.message) : '禁用失败，请稍后重试')
    }
  }

  return (
    <PageLayout variant="admin">
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Seal text="码" />
          <h1 className="font-display text-xl text-text-primary">邀请码管理</h1>
        </div>
        <InkStroke className="w-32 opacity-60" />
        <p className="text-sm text-text-secondary mt-3">
          注册必须凭邀请码。这里的「已用尽」就是用户看到的那个失败原因。
        </p>
      </div>

      {msg && (
        <p className="text-sm mb-4 px-3 py-2 rounded-md break-all" style={{ color: '#3F7F68', background: 'rgba(74,155,127,0.12)' }}>
          {msg}
        </p>
      )}
      {err && (
        <p className="text-sm mb-4 px-3 py-2 rounded-md" style={{ color: '#B93A3A', background: 'rgba(185,58,58,0.10)' }}>
          {err}
        </p>
      )}

      <div className="flex flex-wrap items-center gap-3 mb-4">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
        >
          <option value="">全部状态</option>
          <option value="ACTIVE">可用</option>
          <option value="EXHAUSTED">已用尽</option>
          <option value="EXPIRED">已过期</option>
          <option value="DISABLED">已禁用</option>
        </select>

        <button className="btn-ink text-xs" onClick={() => setShowCreate((v) => !v)}>
          {showCreate ? '取消' : '新建邀请码'}
        </button>

        <button className="btn-ink text-xs" onClick={reload}>
          刷新
        </button>
      </div>

      {showCreate && (
        <div className="panel-ink p-4 mb-5 max-w-md">
          <h2 className="font-display text-sm text-ochre mb-3">新建邀请码</h2>
          <div className="flex flex-col gap-3">
            <label className="text-xs text-text-secondary">
              类型
              <select
                value={newType}
                onChange={(e) => setNewType(e.target.value as 'PLAYER' | 'ADMIN')}
                className="mt-1 w-full bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
              >
                <option value="PLAYER">玩家（USER 角色）</option>
                <option value="ADMIN">管理员（ADMIN 角色）</option>
              </select>
            </label>
            <label className="text-xs text-text-secondary">
              可用次数（1-1000）
              <input
                value={newMaxUses}
                onChange={(e) => setNewMaxUses(e.target.value)}
                className="mt-1 w-full bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
              />
            </label>
            <label className="text-xs text-text-secondary">
              原因（必填，写进审计）
              <input
                value={newReason}
                onChange={(e) => setNewReason(e.target.value)}
                placeholder="例如：给新加入的成员发码"
                className="mt-1 w-full bg-surface-dark border border-ink-600 text-text-primary px-3 py-1 text-sm rounded-md outline-none focus:border-indigo"
              />
            </label>
            <button className="btn-ink text-xs self-start" onClick={handleCreate} disabled={creating}>
              {creating ? '创建中…' : '创建'}
            </button>
          </div>
        </div>
      )}

      {loading && <LoadingState />}
      {!loading && rows.length === 0 && <EmptyState title="没有符合条件的邀请码" />}

      {!loading && rows.length > 0 && (
        <>
          <DataTable
            columns={[
              { key: 'codePrefix', header: '前缀', render: (row: InvitationRow) => row.codePrefix },
              { key: 'type', header: '类型', render: (row: InvitationRow) => row.type },
              {
                key: 'status',
                header: '状态',
                render: (row: InvitationRow) => (
                  <InvitationStatus status={row.status} />
                ),
              },
              {
                key: 'usage',
                header: '使用情况',
                render: (row: InvitationRow) => (
                  <span className={row.usedCount >= row.maxUses ? 'text-ochre' : ''}>
                    {row.usedCount} / {row.maxUses}
                  </span>
                ),
              },
              {
                key: 'expiresAt',
                header: '过期时间',
                render: (row: InvitationRow) => {
                  if (!row.expiresAt) return '不过期'
                  const t = row.expiresAt
                  // ISO 串取到分钟即可，完整值悬停可看
                  const short = t.includes('T') ? t.slice(0, 16).replace('T', ' ') : t
                  return <span title={t}>{short}</span>
                },
              },
              {
                key: 'createdBy',
                header: '创建人',
                // 创建人可能是 usr_<uuid> 这种长串，直接铺开会把整列撑宽、
                // 表格变得稀疏。截断显示，完整值放 title 里悬停可看。
                render: (row: InvitationRow) => {
                  const v = row.createdBy ?? ''
                  const short = v.length > 14 ? `${v.slice(0, 11)}…` : v
                  return (
                    <span title={v} className="font-mono text-xs">
                      {short}
                    </span>
                  )
                },
              },
              {
                key: 'actions',
                header: '操作',
                render: (row: InvitationRow) => (
                  <button
                    className="text-xs underline disabled:opacity-40"
                    disabled={row.status !== 'ACTIVE'}
                    onClick={() => handleDisable(row)}
                  >
                    禁用
                  </button>
                ),
              },
            ]}
            data={rows}
            rowKey={(row: InvitationRow) => row.id}
          />
          <Pagination page={page} pageSize={pageSize} total={data?.total ?? 0} onChange={goTo} />
        </>
      )}
    </PageLayout>
  )
}