import {
  Activity,
  Bell,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Database,
  Download,
  FileText,
  Gauge,
  Home,
  KeyRound,
  Map,
  Menu,
  Server,
  ShieldCheck,
  TriangleAlert,
  Users
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { API_REGISTRY_TOTAL } from "./api/registry";
import { requestEnvelope } from "./api/client";
import type { AdminData, CurrentUser, DashboardData, OpsData, UserRole } from "./api/client";

type RouteKey =
  | "home"
  | "resources"
  | "guides"
  | "login"
  | "me"
  | "admin"
  | "ops"
  | "status"
  | "members"
  | "community"
  | "activity";

const pageLinks: Array<{ label: string; href: string; icon: ReactNode }> = [
  { label: "首页", href: "/", icon: <Home size={18} /> },
  { label: "资源中心", href: "/resources", icon: <Download size={18} /> },
  { label: "指南知识库", href: "/guides", icon: <BookOpen size={18} /> },
  { label: "服务器状态", href: "/status", icon: <Gauge size={18} /> },
  { label: "成员", href: "/members", icon: <Users size={18} /> },
  { label: "社区", href: "/community", icon: <FileText size={18} /> },
  { label: "登录", href: "/login", icon: <KeyRound size={18} /> }
];

const defaultDashboard: DashboardData = {
  contentTitle: "北冥项目门户",
  serverStatus: "UP",
  onlinePlayers: 64,
  resourcesTotal: 18,
  guidesTotal: 29,
  activitiesTotal: 6,
  changelogTitle: "插件与规则更新",
  degradedModules: [],
  requestId: "req_mock"
};

const defaultAdmin: AdminData = {
  modules: ["AUTH", "PROFILE", "CONTENT", "SERVER_STATUS", "RESOURCE", "ADMIN", "GUIDE", "OPS_CONTROL"],
  todos: 18,
  audits: 42,
  platformDependencies: ["API_GATEWAY"],
  degradedModules: [],
  requestId: "req_mock"
};

const defaultOps: OpsData = {
  mode: "SIMULATED",
  nodes: 3,
  assets: 21,
  tasks: 7,
  approvals: 2,
  requestId: "req_mock"
};

function withMock(href: string) {
  const params = new URLSearchParams(window.location.search);
  const mock = params.get("mock");
  return mock ? `${href}?mock=${mock}` : href;
}

function routeKey(): RouteKey {
  const path = window.location.pathname;
  if (path.startsWith("/resources")) return "resources";
  if (path.startsWith("/guides")) return "guides";
  if (path.startsWith("/login") || path.startsWith("/register")) return "login";
  if (path.startsWith("/me")) return "me";
  if (path.startsWith("/admin")) return "admin";
  if (path.startsWith("/ops")) return "ops";
  if (path.startsWith("/status")) return "status";
  if (path.startsWith("/members")) return "members";
  if (path.startsWith("/community")) return "community";
  if (path.startsWith("/activity")) return "activity";
  return "home";
}

export default function App() {
  const [route, setRoute] = useState<RouteKey>(() => routeKey());

  useEffect(() => {
    const onPopState = () => setRoute(routeKey());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(event: React.MouseEvent<HTMLAnchorElement>, href: string) {
    event.preventDefault();
    window.history.pushState({}, "", withMock(href));
    setRoute(routeKey());
  }

  return (
    <main className="app-shell">
      <header className="topbar" data-layout-check="topbar">
        <a className="brand" href={withMock("/")} onClick={(event) => navigate(event, "/")}>
          <span className="brand-mark">
            <Server size={24} />
          </span>
          <span>
            <strong>北冥官网</strong>
            <small>Beiming Official Website</small>
          </span>
        </a>
        <nav className="primary-nav" aria-label="主导航">
          {pageLinks.map((item) => (
            <a
              key={item.href}
              href={withMock(item.href)}
              onClick={(event) => navigate(event, item.href)}
              className={route === routeKeyFromHref(item.href) ? "active" : ""}
            >
              {item.icon}
              {item.label}
            </a>
          ))}
        </nav>
        <div className="topbar-actions">
          <a href={withMock("/me")} onClick={(event) => navigate(event, "/me")}>
            <Bell size={18} />
            用户中心
          </a>
          <a href={withMock("/admin")} onClick={(event) => navigate(event, "/admin")}>
            <ShieldCheck size={18} />
            后台
          </a>
          <a href={withMock("/ops")} onClick={(event) => navigate(event, "/ops")}>
            <Menu size={18} />
            运维
          </a>
        </div>
      </header>

      {route === "home" && <HomePage navigate={navigate} />}
      {route === "resources" && <ResourcePage />}
      {route === "guides" && <GuidePage />}
      {route === "login" && <LoginPage />}
      {route === "me" && <MePage />}
      {route === "admin" && <AdminPage />}
      {route === "ops" && <OpsPage />}
      {route === "status" && <SimplePage title="服务器状态" icon={<Gauge />} module="SERVER_STATUS" />}
      {route === "members" && <SimplePage title="成员档案" icon={<Users />} module="PROFILE" />}
      {route === "community" && <SimplePage title="社区" icon={<FileText />} module="COMMUNITY" />}
      {route === "activity" && <SimplePage title="活动日历" icon={<CalendarDays />} module="ACTIVITY" />}
    </main>
  );
}

function routeKeyFromHref(href: string): RouteKey {
  if (href === "/") return "home";
  if (href.startsWith("/resources")) return "resources";
  if (href.startsWith("/guides")) return "guides";
  if (href.startsWith("/login")) return "login";
  if (href.startsWith("/status")) return "status";
  if (href.startsWith("/members")) return "members";
  if (href.startsWith("/community")) return "community";
  return "activity";
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" ? (value as Record<string, unknown>) : {};
}

function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

function asNumber(value: unknown, fallback: number) {
  return typeof value === "number" ? value : fallback;
}

function adaptContentHome(data: unknown, fallback: DashboardData): DashboardData {
  const view = asRecord(data);
  const sections = asArray(view.sections);
  return {
    ...fallback,
    contentTitle: sections.map((section) => asRecord(section).title).find((title): title is string => typeof title === "string") ?? fallback.contentTitle,
    changelogTitle: typeof view.homeConfigId === "string" ? `首页配置 ${view.homeConfigId}` : fallback.changelogTitle,
    degradedModules: view.degraded === true ? ["CONTENT"] : fallback.degradedModules,
    requestId: typeof view.requestId === "string" ? view.requestId : fallback.requestId
  };
}

function adaptServerStatusOverview(data: unknown, fallback: DashboardData): DashboardData {
  const view = asRecord(data);
  return {
    ...fallback,
    serverStatus: typeof view.overallStatus === "string" ? view.overallStatus : fallback.serverStatus,
    onlinePlayers: asNumber(view.onlinePlayers, fallback.onlinePlayers),
    degradedModules: view.degraded === true ? Array.from(new Set([...fallback.degradedModules, "SERVER_STATUS"])) : fallback.degradedModules
  };
}

function adaptResourcePageTotal(data: unknown, fallback: DashboardData): DashboardData {
  const view = asRecord(data);
  return {
    ...fallback,
    resourcesTotal: asNumber(view.total, fallback.resourcesTotal)
  };
}

function adaptGuideHome(data: unknown, fallback: DashboardData): DashboardData {
  const view = asRecord(data);
  return {
    ...fallback,
    guidesTotal: asArray(view.featuredGuides).length || fallback.guidesTotal,
    degradedModules: view.degraded === true ? Array.from(new Set([...fallback.degradedModules, "GUIDE"])) : fallback.degradedModules
  };
}

function adaptCurrentUser(data: unknown, fallback: CurrentUser): CurrentUser {
  const view = asRecord(data);
  const user = asRecord(view.user);
  return {
    ...fallback,
    id: typeof user.id === "string" ? user.id : fallback.id,
    username: typeof user.username === "string" ? user.username : fallback.username,
    displayName: typeof user.displayName === "string" ? user.displayName : fallback.displayName,
    roles: asArray(user.roles).filter((role): role is UserRole => ["OWNER", "ADMIN", "HELPER", "USER"].includes(String(role))),
    permissions: asArray(user.permissions).map(String),
    status: typeof user.status === "string" ? user.status : fallback.status
  };
}

function adaptAdminOverview(data: unknown, fallback: AdminData): AdminData {
  const view = asRecord(data);
  const todoSummary = asRecord(view.todoSummary);
  return {
    modules: asArray(view.modules).map((module) => asRecord(module).moduleKey).filter((module): module is string => typeof module === "string"),
    todos: asNumber(todoSummary.total, fallback.todos),
    audits: asArray(view.recentAudits).length || fallback.audits,
    platformDependencies: asArray(view.platformDependencies).map((item) => asRecord(item).key).filter((key): key is string => typeof key === "string"),
    degradedModules: asArray(view.degradedModules).map(String),
    requestId: fallback.requestId
  };
}

function adaptOpsOverview(data: unknown, fallback: OpsData): OpsData {
  const view = asRecord(data);
  return {
    mode: asArray(view.degradedModules).includes("NODE_DAEMON") ? "SIMULATED" : fallback.mode,
    nodes: asNumber(view.nodesTotal, fallback.nodes),
    assets: asNumber(view.assetsTotal, fallback.assets),
    tasks: asNumber(view.runningTasksTotal, fallback.tasks),
    approvals: asNumber(view.pendingApprovalsTotal, fallback.approvals),
    requestId: fallback.requestId
  };
}

function HomePage({ navigate }: { navigate: (event: React.MouseEvent<HTMLAnchorElement>, href: string) => void }) {
  const [data, setData] = useState(defaultDashboard);

  useEffect(() => {
    let active = true;
    async function loadDashboard() {
      const content = await requestEnvelope<DashboardData>("/api/v1/content/home", defaultDashboard, adaptContentHome);
      const status = await requestEnvelope<DashboardData>(
        "/api/v1/server-status/overview",
        content.data,
        adaptServerStatusOverview
      );
      const resources = await requestEnvelope<DashboardData>(
        "/api/v1/resources?page=1&pageSize=1",
        status.data,
        adaptResourcePageTotal
      );
      const guides = await requestEnvelope<DashboardData>("/api/v1/guides/home", resources.data, adaptGuideHome);
      if (active) {
        setData(guides.data);
      }
    }
    loadDashboard();
    return () => {
      active = false;
    };
  }, []);

  const degraded = data.degradedModules.length > 0;

  return (
    <section className="page">
      <section className="hero-band">
        <div className="hero-copy">
          <span className="eyebrow">统一入口</span>
          <h1>北冥官网</h1>
          <p>{data.contentTitle}</p>
          <div className="hero-actions">
            <a className="primary-action" href={withMock("/guides")} onClick={(event) => navigate(event, "/guides")}>
              <BookOpen size={18} />
              指南知识库
            </a>
            <a className="secondary-action" href={withMock("/resources")} onClick={(event) => navigate(event, "/resources")}>
              <Download size={18} />
              资源中心
            </a>
          </div>
        </div>
        <div className="hero-visual" aria-label="Minecraft 风格服务器场景" />
      </section>

      {degraded && (
        <section className="degrade-banner">
          <TriangleAlert size={20} />
          <div>
            <strong>局部降级</strong>
            <span>{data.degradedModules.join("、")} 暂不可用，requestId {data.requestId}</span>
          </div>
        </section>
      )}

      <section className="metric-grid" data-layout-check="mobile-stack">
        <Metric icon={<Gauge />} label="服务器" value={data.serverStatus} hint={`${data.onlinePlayers} 在线`} />
        <Metric icon={<Download />} label="资源" value={String(data.resourcesTotal)} hint="版本和下载入口" />
        <Metric icon={<BookOpen />} label="指南" value={String(data.guidesTotal)} hint="规则、指令、入服" />
        <Metric icon={<CalendarDays />} label="活动" value={String(data.activitiesTotal)} hint={data.changelogTitle} />
      </section>

      <section className="workflow-band">
        <Feature title="入服流程" body="账号、资料、规则、考试和白名单都按后端状态推进。" icon={<ClipboardList />} />
        <Feature title="后台入口" body="待办、审计、指标和模块健康只做聚合，不代替业务模块。" icon={<Database />} />
        <Feature title="运维控制" body="节点、资产、任务和审批分开展示，模拟状态不会写成真实成功。" icon={<Server />} />
      </section>
    </section>
  );
}

function ResourcePage() {
  return (
    <DetailShell title="资源中心" icon={<Download />} module="RESOURCE">
      <DataStrip items={["公开资源列表", "资源分类", "版本列表", "下载解析"]} />
      <p>资源页只请求 `resource` 正式接口。下载动作必须先拿后端票据，Cloudreve 降级时只显示暂不可用。</p>
    </DetailShell>
  );
}

function GuidePage() {
  return (
    <DetailShell title="指南知识库" icon={<BookOpen />} module="GUIDE">
      <DataStrip items={["分类", "搜索", "当前规则", "外部入口"]} />
      <p>规则确认、外部交流入口、指令索引和反馈都来自 `guide` 契约。页面不做跨平台登录。</p>
    </DetailShell>
  );
}

function LoginPage() {
  return (
    <section className="page narrow">
      <h1>登录</h1>
      <form className="auth-form">
        <label>
          <span>用户名</span>
          <input defaultValue="owner" autoComplete="username" />
        </label>
        <label>
          <span>密码</span>
          <input defaultValue="Password12345" type="password" autoComplete="current-password" />
        </label>
        <button type="button">
          <KeyRound size={18} />
          登录
        </button>
      </form>
    </section>
  );
}

function MePage() {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loaded, setLoaded] = useState(false);
  useEffect(() => {
    requestEnvelope<CurrentUser>("/api/v1/auth/session/verify", {
      id: "user_owner",
      username: "owner",
      displayName: "北冥管理者",
      roles: ["OWNER"],
      permissions: ["NODE_READ", "HIGH_RISK_APPROVE"],
      status: "ACTIVE"
    }, adaptCurrentUser).then((response) => {
      setUser(response.code === 0 ? response.data : null);
      setLoaded(true);
    });
  }, []);

  return (
    <DetailShell title="用户中心" icon={<Users />} module="AUTH">
      <DataStrip items={["当前用户", "成员档案", "通知", "入服流程", "考勤"]} />
      <p>{user ? `${user.displayName} · ${user.roles.join(" / ")}` : loaded ? "需要登录" : "加载中"}</p>
    </DetailShell>
  );
}

function AdminPage() {
  const [data, setData] = useState(defaultAdmin);
  const [adminError, setAdminError] = useState<string | null>(null);
  useEffect(() => {
    requestEnvelope<AdminData>("/api/v1/admin/overview", defaultAdmin, adaptAdminOverview).then((response) => {
      if (response.code === 0) {
        setData(response.data);
        setAdminError(null);
      } else {
        setAdminError(response.requestId ?? "req_frontend_admin");
      }
    });
  }, []);
  return (
    <section className="page">
      <PageTitle icon={<ShieldCheck />} title="管理后台" subtitle="总览、模块、待办、审计和平台依赖保持分离。" />
      {adminError ? (
        <section className="degrade-banner">
          <TriangleAlert size={20} />
          <div>
            <strong>后台暂不可用</strong>
            <span>认证失败或服务降级，requestId {adminError}</span>
          </div>
        </section>
      ) : (
        <>
          <section className="metric-grid" data-layout-check="mobile-stack">
            <Metric icon={<Database />} label="模块" value={String(data.modules.length)} hint="已闭环服务入口" />
            <Metric icon={<ClipboardList />} label="待办" value={String(data.todos)} hint="只读聚合" />
            <Metric icon={<Activity />} label="审计" value={String(data.audits)} hint="只读索引" />
            <Metric icon={<Server />} label="平台" value={data.platformDependencies.join(" / ")} hint="API_GATEWAY" />
          </section>
          <section className="module-list">
            {data.platformDependencies.map((item) => (
              <span key={item}>{item}</span>
            ))}
            {data.modules.map((item) => (
              <span key={item}>{item}</span>
            ))}
          </section>
        </>
      )}
      {data.degradedModules.length > 0 && (
        <section className="degrade-banner">
          <TriangleAlert size={20} />
          <div>
            <strong>局部降级</strong>
            <span>{data.degradedModules.join("、")} · requestId {data.requestId}</span>
          </div>
        </section>
      )}
    </section>
  );
}

function OpsPage() {
  const [data, setData] = useState(defaultOps);
  useEffect(() => {
    requestEnvelope<OpsData>("/api/v1/ops-control/overview", defaultOps, adaptOpsOverview).then((response) => setData(response.data));
  }, []);
  return (
    <section className="page">
      <PageTitle icon={<Server />} title="运维控制台" subtitle="控制面只展示受控摘要，真实执行交给节点守护进程。" />
      <section className="metric-grid" data-layout-check="mobile-stack">
        <Metric icon={<CheckCircle2 />} label="模式" value={data.mode} hint="模拟状态明确展示" />
        <Metric icon={<Server />} label="节点" value={String(data.nodes)} hint="NODE_READ" />
        <Metric icon={<Database />} label="资产" value={String(data.assets)} hint="服务器、实例、云盘" />
        <Metric icon={<TriangleAlert />} label="审批" value={String(data.approvals)} hint="高风险操作" />
      </section>
      <section className="risk-panel">
        <strong>高风险操作需要二次确认</strong>
        <button type="button">输入来源契约确认文本</button>
      </section>
    </section>
  );
}

function SimplePage({ title, icon, module }: { title: string; icon: ReactNode; module: string }) {
  return (
    <DetailShell title={title} icon={icon} module={module}>
      <DataStrip items={["列表", "详情", "空状态", "降级", "审计入口"]} />
      <p>{module} 页面只读取对应正式 API。接口失败时保留 requestId 并进入局部状态。</p>
    </DetailShell>
  );
}

function DetailShell({ title, icon, module, children }: { title: string; icon: ReactNode; module: string; children: ReactNode }) {
  const matched = useMemo(() => API_REGISTRY_TOTAL, []);
  return (
    <section className="page">
      <PageTitle icon={icon} title={title} subtitle={`${module} · registry 覆盖 ${matched} 个契约接口`} />
      <section className="detail-band">{children}</section>
    </section>
  );
}

function PageTitle({ icon, title, subtitle }: { icon: ReactNode; title: string; subtitle: string }) {
  return (
    <header className="page-title">
      <span>{icon}</span>
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
    </header>
  );
}

function Metric({ icon, label, value, hint }: { icon: ReactNode; label: string; value: string; hint: string }) {
  return (
    <article className="metric-card">
      <span className="metric-icon">{icon}</span>
      <small>{label}</small>
      <strong>{value}</strong>
      <span>{hint}</span>
    </article>
  );
}

function Feature({ title, body, icon }: { title: string; body: string; icon: ReactNode }) {
  return (
    <article className="feature-row">
      <span>{icon}</span>
      <div>
        <strong>{title}</strong>
        <p>{body}</p>
      </div>
    </article>
  );
}

function DataStrip({ items }: { items: string[] }) {
  return (
    <div className="data-strip">
      {items.map((item) => (
        <span key={item}>{item}</span>
      ))}
    </div>
  );
}
