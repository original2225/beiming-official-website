/**
 * 官网首页 — 全屏视差滚动，中式水墨风
 */

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoadingState } from '../../components/feedback/LoadingState'
import { LoginModal } from '../../components/common/LoginModal'
import { RegisterModal } from '../../components/common/RegisterModal'
import { EmptyState } from '../../components/feedback/EmptyState'
import { TimeDisplay } from '../../components/data-display/TimeDisplay'
import { useRequest } from '../../hooks/useRequest'
import * as pub from '../../api/modules/public'
import { ROUTES } from '../../constants/routes'
import { img } from '../../constants/images'
import type { ServerStatusSummary, ContentSummary, GuideSummary, ResourceSummary, MemberSummary, ActivitySummary, ChangelogSummary } from '../../types/domain'
import type { PageResult } from '../../types/api'

// ═══════════════════════════════════════════════
// 视差区块容器
// ═══════════════════════════════════════════════

function ParallaxSection({
  bgImage,
  children,
  className = '',
  id,
}: {
  bgImage: string
  children: React.ReactNode
  className?: string
  id?: string
}) {
  const isGradient = bgImage.includes('gradient') || bgImage.startsWith('url(')
  return (
    <section
      id={id}
      className={`relative min-h-screen flex items-center bg-fixed bg-cover bg-center ${className}`}
      style={{
        backgroundImage: isGradient ? bgImage : `url(${bgImage})`,
        backgroundAttachment: 'fixed',
      }}
    >
      <div className="absolute inset-0 bg-ink-900/50" />
      <div className="relative z-10 max-w-6xl mx-auto px-6 w-full py-24">
        {children}
      </div>
    </section>
  )
}

// ═══════════════════════════════════════════════
// Hero 首屏
// ═══════════════════════════════════════════════

function HeroSection() {
  return (
    <section
      className="relative h-screen flex items-center justify-center bg-cover bg-center"
      style={{
        backgroundImage: `url(${img('heroBg')})`,
        backgroundAttachment: 'fixed',
      }}
    >
      <div className="absolute inset-0 bg-gradient-to-b from-ink-900/70 via-ink-900/40 to-ink-900/70" />
      <div className="relative z-10 text-center">
        <h1 className="font-calligraphy text-7xl md:text-9xl text-rice-50 tracking-[0.2em] drop-shadow-lg">
          北冥
        </h1>
        <p className="font-display text-ochre text-xl md:text-2xl mt-4 tracking-widest">
          山水之间，自有江湖
        </p>
        <p className="text-text-secondary text-sm mt-6 max-w-md mx-auto leading-relaxed">
          欢迎来到北冥世界 · 创造属于你的方块传奇
        </p>
        <div className="mt-8 flex gap-4 justify-center">
          <Link to={ROUTES.GUIDES} className="btn-ink text-sm">新手入门</Link>
          <Link to={ROUTES.BOARDS} className="btn-ink-ghost text-sm">
            加入社区
          </Link>
        </div>
      </div>
    </section>
  )
}

// ═══════════════════════════════════════════════
// 服务器状态
// ═══════════════════════════════════════════════

function StatusBlock() {
  const { data, loading, run } = useRequest<ServerStatusSummary>()

  useEffect(() => { run(() => pub.getServerOverview()) }, [run])

  return (
    <ParallaxSection bgImage={img('bgStatus')} id="status">
      <div className="panel-ink p-8 max-w-2xl mx-auto text-center">
        <h2 className="font-display text-3xl text-indigo mb-8 tracking-widest">服务器状态</h2>
        {loading && <LoadingState />}
        {data && (
          <div className="flex items-center justify-center gap-12">
            <div className="text-center">
              <div className={`w-5 h-5 mx-auto mb-2 rounded-full ${data.online ? 'bg-jade shadow-[0_0_12px_rgba(74,155,127,0.8)]' : 'bg-cinnabar shadow-[0_0_12px_rgba(185,58,58,0.8)]'}`} />
              <span className="font-display text-2xl">{data.online ? '在线' : '离线'}</span>
            </div>
            {data.online && (
              <>
                <div className="text-center">
                  <p className="font-display text-3xl text-ochre">{data.playerCount}</p>
                  <p className="text-xs text-text-muted mt-1">当前玩家</p>
                </div>
                <div className="text-center">
                  <p className="font-display text-2xl text-text-primary">{data.version}</p>
                  <p className="text-xs text-text-muted mt-1">游戏版本</p>
                </div>
              </>
            )}
          </div>
        )}
        {data?.online && (
          <p className="text-sm text-text-muted mt-4 italic">"{data.motd}"</p>
        )}
      </div>
    </ParallaxSection>
  )
}

// ═══════════════════════════════════════════════
// 公告
// ═══════════════════════════════════════════════

function AnnounceBlock() {
  const { data, loading, run } = useRequest<PageResult<ContentSummary>>()

  useEffect(() => { run(() => pub.getContentItems({ pageSize: 4 })) }, [run])

  return (
    <ParallaxSection bgImage={img('bgAnnounce')} id="announce">
      <div className="text-center mb-10">
        <h2 className="font-display text-3xl text-ochre tracking-widest">公告</h2>
        <p className="text-text-muted text-sm mt-2">ANNOUNCEMENTS</p>
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无公告" />}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-4xl mx-auto">
        {data?.items.map((item) => (
          <Link
            key={item.contentId}
            to={`/announcements/${item.contentId}`}
            className="block panel-ink p-5 hover:border-indigo transition-all group"
          >
            <h3 className="font-display text-sm text-text-primary group-hover:text-indigo transition-colors">{item.title}</h3>
            {item.publishedAt && (
              <p className="text-xs text-text-muted mt-2"><TimeDisplay iso={item.publishedAt} /></p>
            )}
          </Link>
        ))}
      </div>
    </ParallaxSection>
  )
}

// ═══════════════════════════════════════════════
// 指南 + 资源
// ═══════════════════════════════════════════════

function GuidesResourcesBlock() {
  const { data: guides, loading: gLoading, run: gRun } = useRequest<PageResult<GuideSummary>>()
  const { data: resources, loading: rLoading, run: rRun } = useRequest<PageResult<ResourceSummary>>()

  useEffect(() => { gRun(() => pub.getGuides({ pageSize: 4 })) }, [gRun])
  useEffect(() => { rRun(() => pub.getResources({ pageSize: 4 })) }, [rRun])

  return (
    <ParallaxSection bgImage={img('bgHero')} id="guides">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        {/* 指南 */}
        <div>
          <div className="mb-6">
            <h2 className="font-display text-2xl text-indigo tracking-widest">指南中心</h2>
            <p className="text-text-muted text-xs mt-1">GUIDES</p>
          </div>
          {gLoading && <LoadingState />}
          {!gLoading && !guides?.items?.length && <EmptyState text="暂无指南" />}
          <div className="flex flex-col gap-2">
            {guides?.items.map((g) => (
              <Link key={g.guideId} to={`/guides/${g.guideId}`}
                className="border-l-2 border-ink-600/30 pl-4 py-2 hover:border-indigo transition-colors group rounded-r-md">
                <span className="text-sm text-text-secondary group-hover:text-text-primary">{g.title}</span>
                <span className="text-xs text-text-muted ml-2">{g.category}</span>
              </Link>
            ))}
          </div>
        </div>

        {/* 资源 */}
        <div>
          <div className="mb-6">
            <h2 className="font-display text-2xl text-ochre tracking-widest">资源下载</h2>
            <p className="text-text-muted text-xs mt-1">RESOURCES</p>
          </div>
          {rLoading && <LoadingState />}
          {!rLoading && !resources?.items?.length && <EmptyState text="暂无资源" />}
          <div className="grid grid-cols-2 gap-2">
            {resources?.items.map((r) => (
              <Link key={r.resourceId} to={`/resources/${r.resourceId}`}
                className="block panel-ink p-3 hover:border-indigo transition-all text-center group">
                <p className="font-display text-xs text-text-primary group-hover:text-indigo">{r.title}</p>
                <p className="text-xs text-text-muted mt-1">{r.category}</p>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </ParallaxSection>
  )
}

// ═══════════════════════════════════════════════
// 成员展示
// ═══════════════════════════════════════════════

function MembersBlock() {
  const { data, loading, run } = useRequest<PageResult<MemberSummary>>()

  useEffect(() => { run(() => pub.getMembers({ pageSize: 8 })) }, [run])

  return (
    <ParallaxSection bgImage={img('bgMembers')} id="members">
      <div className="text-center mb-10">
        <h2 className="font-display text-3xl text-indigo tracking-widest">社区成员</h2>
        <p className="text-text-muted text-sm mt-2">COMMUNITY MEMBERS</p>
      </div>
      {loading && <LoadingState />}
      {!loading && !data?.items.length && <EmptyState text="暂无成员" />}
      <div className="flex flex-wrap justify-center gap-4 max-w-4xl mx-auto">
        {data?.items.map((m) => (
          <div key={m.memberId} className="w-32 text-center">
            <div className="w-16 h-16 mx-auto rounded-full border border-indigo bg-surface-light flex items-center justify-center mb-2">
              <span className="font-display text-2xl text-indigo">{m.displayName[0]}</span>
            </div>
            <p className="font-display text-xs text-text-primary truncate">{m.displayName}</p>
            {m.minecraftName && <p className="text-xs text-indigo truncate">{m.minecraftName}</p>}
          </div>
        ))}
      </div>
    </ParallaxSection>
  )
}

// ═══════════════════════════════════════════════
// 活动 + 更新
// ═══════════════════════════════════════════════

function ActivityChangelogBlock() {
  const { data: activities, loading: aLoading, run: aRun } = useRequest<PageResult<ActivitySummary>>()
  const { data: changelogs, loading: cLoading, run: cRun } = useRequest<PageResult<ChangelogSummary>>()

  useEffect(() => { aRun(() => pub.getActivities({ pageSize: 3 })) }, [aRun])
  useEffect(() => { cRun(() => pub.getChangelogReleases({ pageSize: 3 })) }, [cRun])

  return (
    <ParallaxSection bgImage={img('texScene')}>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        <div>
          <div className="mb-6">
            <h2 className="font-display text-2xl text-indigo tracking-widest">近期活动</h2>
            <p className="text-text-muted text-xs mt-1">EVENTS</p>
          </div>
          {aLoading && <LoadingState />}
          {!aLoading && !activities?.items?.length && <EmptyState text="暂无活动" />}
          {activities?.items.map((a) => (
            <Link key={a.activityId} to={`/activities/${a.activityId}`}
              className="flex items-center justify-between border-l-2 border-ink-600/30 pl-4 py-2 mb-1 hover:border-indigo transition-colors rounded-r-md">
              <span className="text-sm text-text-secondary">{a.title}</span>
              {a.registrationOpen && <span className="text-xs text-jade">报名中</span>}
            </Link>
          ))}
        </div>

        <div>
          <div className="mb-6">
            <h2 className="font-display text-2xl text-ochre tracking-widest">版本更新</h2>
            <p className="text-text-muted text-xs mt-1">CHANGELOG</p>
          </div>
          {cLoading && <LoadingState />}
          {!cLoading && !changelogs?.items?.length && <EmptyState text="暂无更新" />}
          {changelogs?.items.map((c) => (
            <Link key={c.releaseId} to={`/changelog/${c.releaseId}`}
              className="flex items-center gap-3 border-l-2 border-ink-600/30 pl-4 py-2 mb-1 hover:border-indigo transition-colors rounded-r-md">
              <span className="font-display text-sm text-ochre">{c.version}</span>
              <span className="text-sm text-text-secondary">{c.title}</span>
            </Link>
          ))}
        </div>
      </div>

      <div className="text-center mt-12 pt-8 border-t border-ink-600/30">
        <Link to={ROUTES.CALENDAR} className="font-display text-indigo text-sm hover:text-ochre transition-colors tracking-widest">
          查看完整日程 →
        </Link>
      </div>
    </ParallaxSection>
  )
}

// ═══════════════════════════════════════════════
// 页脚
// ═══════════════════════════════════════════════

function FooterBlock() {
  return (
    <footer className="bg-surface-dark border-t border-ink-600/30 py-12 text-center">
      <p className="font-calligraphy text-3xl text-indigo tracking-widest mb-2">北冥</p>
      <p className="text-xs text-text-muted">BEIMING SERVER</p>
      <div className="flex justify-center gap-6 mt-4 text-xs text-text-muted">
        <Link to={ROUTES.GUIDES} className="hover:text-indigo">指南</Link>
        <Link to={ROUTES.BOARDS} className="hover:text-indigo">社区</Link>
        <Link to={ROUTES.CHANGELOG} className="hover:text-indigo">更新日志</Link>
      </div>
      <p className="text-xs text-text-muted mt-6">© 2026 Beiming Server</p>
    </footer>
  )
}

// ═══════════════════════════════════════════════
// 首页
// ═══════════════════════════════════════════════

export function HomePage() {
  const [loginOpen, setLoginOpen] = useState(false)
  const [registerOpen, setRegisterOpen] = useState(false)
  const openRegister = () => { setLoginOpen(false); setRegisterOpen(true) }
  const openLogin = () => { setRegisterOpen(false); setLoginOpen(true) }

  return (
    <div className="bg-surface-dark">
      {/* 固定顶部导航 */}
      <nav className="fixed top-0 left-0 right-0 z-50 h-14 flex items-center justify-between px-6 bg-ink-900/70 backdrop-blur border-b border-ink-600/20">
        <Link to={ROUTES.HOME} className="font-display text-indigo text-lg tracking-wider">北冥</Link>
        <div className="flex items-center gap-4 text-xs font-body text-text-muted">
          <a href="#status" className="hover:text-indigo">状态</a>
          <a href="#announce" className="hover:text-indigo">公告</a>
          <a href="#guides" className="hover:text-indigo">指南</a>
          <a href="#resources" className="hover:text-indigo">资源</a>
          <a href="#members" className="hover:text-indigo">成员</a>
          <button onClick={() => setLoginOpen(true)} className="hover:text-ochre">登录</button>
        </div>
      </nav>
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} onSwitchToRegister={openRegister} />
      <RegisterModal open={registerOpen} onClose={() => setRegisterOpen(false)} onSwitchToLogin={openLogin} />
      <HeroSection />
      <StatusBlock />
      <AnnounceBlock />
      <GuidesResourcesBlock />
      <MembersBlock />
      <ActivityChangelogBlock />
      <FooterBlock />
    </div>
  )
}
