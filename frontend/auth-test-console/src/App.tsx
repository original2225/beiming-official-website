import {
  Activity,
  BadgeCheck,
  Ban,
  Boxes,
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  Cloud,
  Code2,
  Database,
  KeyRound,
  Link2,
  LogOut,
  Network,
  Plus,
  RefreshCcw,
  Search,
  Server,
  ShieldCheck,
  Ticket,
  UserRound,
  Users
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import type { FormEvent, ReactNode, RefObject } from "react";
import { useRef } from "react";

type ApiEnvelope<T = unknown> = {
  code: number;
  message: string;
  data: T;
  requestId?: string;
  errors?: Array<{ field: string; reason: string }>;
};

type UserSummary = {
  id: string;
  username: string;
  displayName: string;
  roles: string[];
  permissions: string[];
  status: string;
  minecraftBinding: null | {
    minecraftId: string;
    minecraftUuid: string;
    verifiedAt: string;
    source: string;
  };
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
};

type SessionPayload = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserSummary;
};

type PagePayload<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
};

type InvitationSummary = {
  id: string;
  codePrefix: string;
  type: string;
  status: string;
  boundRoles: string[];
  boundPermissions: string[];
  maxUses: number;
  usedCount: number;
  expiresAt: string | null;
  createdBy: string;
  createdAt: string;
  disabledAt: string | null;
};

type CreateInvitationResult = {
  invitation: InvitationSummary;
  rawCode: string;
};

type ServiceStatus = "checking" | "online" | "offline";
type SectionKey = "overview" | "account" | "session" | "invitation" | "logs";

const API_BASE = "http://localhost:8101/api/v1/auth";
const storedToken = localStorage.getItem("beiming.authTestConsole.token") ?? "";

const formatTime = (value?: string | null) => {
  if (!value) {
    return "未返回";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
};

export default function App() {
  const overviewRef = useRef<HTMLElement | null>(null);
  const accountRef = useRef<HTMLElement | null>(null);
  const sessionRef = useRef<HTMLElement | null>(null);
  const invitationRef = useRef<HTMLElement | null>(null);
  const logsRef = useRef<HTMLElement | null>(null);
  const [token, setToken] = useState(storedToken);
  const [currentUser, setCurrentUser] = useState<UserSummary | null>(null);
  const [status, setStatus] = useState<ServiceStatus>("checking");
  const [lastResponse, setLastResponse] = useState<ApiEnvelope | null>(null);
  const [lastAction, setLastAction] = useState("等待请求");
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState<SectionKey>("overview");
  const [registerForm, setRegisterForm] = useState({
    invitationCode: "PLAYER-CODE-1",
    username: `tester_${Date.now().toString().slice(-5)}`,
    password: "Password12345",
    displayName: `测试员${Date.now().toString().slice(-4)}`
  });
  const [loginForm, setLoginForm] = useState({
    username: "owner",
    password: "Password12345"
  });
  const [minecraftForm, setMinecraftForm] = useState({
    minecraftId: "BeimingTest",
    minecraftUuid: "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
    verificationCode: "local"
  });
  const [invitationForm, setInvitationForm] = useState({
    type: "PLAYER",
    boundRoles: "USER",
    boundPermissions: "",
    maxUses: "3",
    reason: "local auth test"
  });
  const [invitations, setInvitations] = useState<InvitationSummary[]>([]);
  const [latestRawCode, setLatestRawCode] = useState("");

  const roleLabel = useMemo(() => currentUser?.roles.join(" / ") ?? "未登录", [currentUser]);
  const tokenPreview = token ? `${token.slice(0, 10)}...${token.slice(-6)}` : "空";

  useEffect(() => {
    localStorage.setItem("beiming.authTestConsole.token", token);
  }, [token]);

  useEffect(() => {
    checkConnection();
    if (token) {
      verifySession();
    }
  }, []);

  async function callApi<T>(
    action: string,
    path: string,
    options: { method?: string; body?: unknown; auth?: boolean } = {}
  ) {
    setBusyAction(action);
    setLastAction(action);
    try {
      const response = await fetch(`${API_BASE}${path}`, {
        method: options.method ?? "GET",
        headers: {
          "Content-Type": "application/json",
          ...(options.auth ? { Authorization: `Bearer ${token}` } : {})
        },
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
      });
      const payload = (await response.json()) as ApiEnvelope<T>;
      setLastResponse(payload);
      setStatus("online");
      return { response, payload };
    } catch (error) {
      const payload: ApiEnvelope = {
        code: -1,
        message: error instanceof Error ? error.message : "request failed",
        data: null
      };
      setLastResponse(payload);
      setStatus("offline");
      return { response: null, payload };
    } finally {
      setBusyAction(null);
    }
  }

  async function checkConnection() {
    setStatus("checking");
    await callApi("服务探测", "/me");
  }

  async function login(event?: FormEvent) {
    event?.preventDefault();
    const { response, payload } = await callApi<SessionPayload>("登录", "/login", {
      method: "POST",
      body: loginForm
    });
    if (response?.ok && payload.data) {
      setToken(payload.data.accessToken);
      setCurrentUser(payload.data.user);
    }
  }

  async function register(event: FormEvent) {
    event.preventDefault();
    const { response, payload } = await callApi<SessionPayload>("注册", "/register", {
      method: "POST",
      body: registerForm
    });
    if (response?.ok && payload.data) {
      setToken(payload.data.accessToken);
      setCurrentUser(payload.data.user);
    }
  }

  async function loadMe() {
    const { response, payload } = await callApi<UserSummary>("当前用户", "/me", { auth: true });
    if (response?.ok && payload.data) {
      setCurrentUser(payload.data);
    }
  }

  async function verifySession() {
    const { response, payload } = await callApi<{ valid: boolean; expiresAt: string; user: UserSummary }>("会话校验", "/session/verify", {
      auth: true
    });
    if (response?.ok && payload.data) {
      setCurrentUser(payload.data.user);
    }
  }

  async function logout() {
    await callApi("退出登录", "/logout", { method: "POST", auth: true });
    setToken("");
    setCurrentUser(null);
  }

  async function bindMinecraft(event: FormEvent) {
    event.preventDefault();
    await callApi("绑定 Minecraft", "/me/minecraft-binding", {
      method: "PUT",
      auth: true,
      body: minecraftForm
    });
    await loadMe();
  }

  async function listUsers() {
    await callApi("后台用户列表", "/admin/users?page=1&pageSize=6", { auth: true });
  }

  async function listInvitations() {
    const { response, payload } = await callApi<PagePayload<InvitationSummary>>("邀请码列表", "/admin/invitations?page=1&pageSize=10", { auth: true });
    if (response?.ok && payload.data) {
      setInvitations(payload.data.items);
    }
  }

  async function createInvitation(event: FormEvent) {
    event.preventDefault();
    const { response, payload } = await callApi<CreateInvitationResult>("创建邀请码", "/admin/invitations", {
      method: "POST",
      auth: true,
      body: {
        type: invitationForm.type,
        boundRoles: csvValues(invitationForm.boundRoles),
        boundPermissions: csvValues(invitationForm.boundPermissions),
        maxUses: Number(invitationForm.maxUses),
        reason: invitationForm.reason,
        idempotencyKey: `ui-${Date.now()}`
      }
    });
    if (response?.ok && payload.data) {
      setLatestRawCode(payload.data.rawCode);
      setInvitations((items) => [payload.data.invitation, ...items.filter((item) => item.id !== payload.data.invitation.id)]);
    }
  }

  async function disableInvitation(invitationId: string) {
    const { response, payload } = await callApi<InvitationSummary>("禁用邀请码", `/admin/invitations/${invitationId}/disable`, {
      method: "PATCH",
      auth: true,
      body: { reason: "disabled from auth test console" }
    });
    if (response?.ok && payload.data) {
      setInvitations((items) => items.map((item) => (item.id === invitationId ? payload.data : item)));
    }
  }

  const responseCode = lastResponse ? String(lastResponse.code) : "NA";
  const online = status === "online";
  const navItems: Array<{ key: SectionKey; label: string; icon: ReactNode; ref: RefObject<HTMLElement | null> }> = [
    { key: "overview", label: "概览", icon: <Boxes size={19} />, ref: overviewRef },
    { key: "account", label: "账号", icon: <Users size={19} />, ref: accountRef },
    { key: "session", label: "会话", icon: <ShieldCheck size={19} />, ref: sessionRef },
    { key: "invitation", label: "邀请码", icon: <Ticket size={19} />, ref: invitationRef },
    { key: "logs", label: "日志", icon: <Code2 size={19} />, ref: logsRef }
  ];

  function goToSection(key: SectionKey, sectionRef: RefObject<HTMLElement | null>) {
    setActiveSection(key);
    sectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <main>
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            <Cloud size={28} />
          </div>
          <div>
            <strong>北冥云</strong>
            <span>Auth 测试台</span>
          </div>
        </div>

        <nav className="tabs" aria-label="测试台导航">
          {navItems.map((item) => (
            <button
              className={`tab ${activeSection === item.key ? "active" : ""}`}
              type="button"
              aria-current={activeSection === item.key ? "page" : undefined}
              onClick={() => goToSection(item.key, item.ref)}
              key={item.key}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </nav>

        <label className="search">
          <Search size={21} />
          <input value="搜索用户、邀请码、会话" readOnly />
        </label>

        <button className="node">
          Auth-8101 - localhost
          <ChevronDown size={20} />
        </button>

        <div className="operator">
          <div className="avatar">北</div>
          <div>
            <strong>{currentUser?.displayName ?? "测试管理员"}</strong>
            <span>{roleLabel}</span>
          </div>
        </div>
      </header>

      <section className="shell">
        <section className="hero" ref={overviewRef}>
          <div className="hero-copy">
            <h1>统一测试注册、登录与会话校验</h1>
            <p>当前节点：Auth-8101 - localhost。测试台只负责调用 auth 契约接口。</p>
            <div className="hero-actions">
              <button className="primary" onClick={() => login()} disabled={busyAction === "登录"}>
                <KeyRound size={20} />
                快速登录
              </button>
              <button className="ghost" onClick={checkConnection} disabled={busyAction === "服务探测"}>
                <RefreshCcw size={19} />
                刷新状态
              </button>
            </div>
          </div>

          <aside className="status-card" ref={sessionRef}>
            <div className="status-head">
              <h2>北冥认证器</h2>
              <b className={online ? "pill online" : status === "checking" ? "pill checking" : "pill offline"}>
                {status.toUpperCase()}
              </b>
            </div>
            <div className="status-grid">
              <Metric label="API" value="8101" />
              <Metric label="SESSION" value={token ? "READY" : "EMPTY"} />
              <Metric label="USER" value={currentUser?.username ?? "NONE"} />
              <Metric label="CODE" value={responseCode} />
            </div>
            <div className="radar" aria-hidden="true" />
          </aside>
        </section>

        <section className="metrics">
          <InfoCard icon={<Server />} label="服务端口" value="8101" hint="Spring Boot" />
          <InfoCard icon={<BadgeCheck />} label="会话状态" value={token ? "已持有" : "未登录"} hint={tokenPreview} />
          <InfoCard icon={<UserRound />} label="当前用户" value={currentUser?.username ?? "Guest"} hint={currentUser?.status ?? "等待登录"} />
          <InfoCard icon={<Database />} label="最近响应" value={responseCode} hint={lastAction} />
        </section>

        <section className="workspace">
          <section className="panel auth-panel" ref={accountRef}>
            <div className="panel-title">
              <Network size={22} />
              <h2>接口操作</h2>
            </div>

            <div className="forms">
              <form onSubmit={login} className="form-card">
                <h3>登录</h3>
                <Field label="用户名" value={loginForm.username} onChange={(username) => setLoginForm({ ...loginForm, username })} />
                <Field label="密码" value={loginForm.password} type="password" onChange={(password) => setLoginForm({ ...loginForm, password })} />
                <button className="primary full" disabled={busyAction === "登录"}>
                  <KeyRound size={18} />
                  登录
                </button>
              </form>

              <form onSubmit={register} className="form-card">
                <h3>注册</h3>
                <Field label="邀请码" value={registerForm.invitationCode} onChange={(invitationCode) => setRegisterForm({ ...registerForm, invitationCode })} />
                <Field label="用户名" value={registerForm.username} onChange={(username) => setRegisterForm({ ...registerForm, username })} />
                <Field label="密码" value={registerForm.password} type="password" onChange={(password) => setRegisterForm({ ...registerForm, password })} />
                <Field label="展示名" value={registerForm.displayName} onChange={(displayName) => setRegisterForm({ ...registerForm, displayName })} />
                <button className="primary full" disabled={busyAction === "注册"}>
                  <UserRound size={18} />
                  注册
                </button>
              </form>

              <form onSubmit={bindMinecraft} className="form-card wide">
                <h3>Minecraft 绑定</h3>
                <Field label="Minecraft ID" value={minecraftForm.minecraftId} onChange={(minecraftId) => setMinecraftForm({ ...minecraftForm, minecraftId })} />
                <Field label="UUID" value={minecraftForm.minecraftUuid} onChange={(minecraftUuid) => setMinecraftForm({ ...minecraftForm, minecraftUuid })} />
                <Field label="验证码" value={minecraftForm.verificationCode} onChange={(verificationCode) => setMinecraftForm({ ...minecraftForm, verificationCode })} />
                <button className="ghost full" disabled={!token || busyAction === "绑定 Minecraft"}>
                  <Link2 size={18} />
                  绑定
                </button>
              </form>
            </div>

            <div className="quick-actions">
              <button onClick={loadMe} disabled={!token || busyAction === "当前用户"}>
                <UserRound size={18} />
                当前用户
              </button>
              <button onClick={verifySession} disabled={!token || busyAction === "会话校验"}>
                <CheckCircle2 size={18} />
                会话校验
              </button>
              <button onClick={listUsers} disabled={!token || busyAction === "后台用户列表"}>
                <Users size={18} />
                用户列表
              </button>
              <button onClick={listInvitations} disabled={!token || busyAction === "邀请码列表"}>
                <KeyRound size={18} />
                邀请码
              </button>
              <button onClick={logout} disabled={!token || busyAction === "退出登录"}>
                <LogOut size={18} />
                退出
              </button>
            </div>

            <section className="invite-console" ref={invitationRef}>
              <div className="panel-title">
                <Ticket size={22} />
                <h2>邀请码管理</h2>
              </div>

              <div className="invite-layout">
                <form onSubmit={createInvitation} className="invite-form">
                  <SelectField
                    label="类型"
                    value={invitationForm.type}
                    options={["PLAYER", "ADMIN"]}
                    onChange={(type) => setInvitationForm({ ...invitationForm, type, boundRoles: type === "ADMIN" ? "ADMIN" : "USER" })}
                  />
                  <Field label="绑定角色" value={invitationForm.boundRoles} onChange={(boundRoles) => setInvitationForm({ ...invitationForm, boundRoles })} />
                  <Field label="能力点" value={invitationForm.boundPermissions} onChange={(boundPermissions) => setInvitationForm({ ...invitationForm, boundPermissions })} />
                  <Field label="次数" type="number" value={invitationForm.maxUses} onChange={(maxUses) => setInvitationForm({ ...invitationForm, maxUses })} />
                  <Field label="原因" value={invitationForm.reason} onChange={(reason) => setInvitationForm({ ...invitationForm, reason })} />
                  <button className="primary full" disabled={!token || busyAction === "创建邀请码"}>
                    <Plus size={18} />
                    创建邀请码
                  </button>
                </form>

                <div className="invite-list">
                  <div className="invite-toolbar">
                    <div>
                      <strong>最近邀请码</strong>
                      <span>{latestRawCode || "创建成功后这里显示完整邀请码，仅本次可见"}</span>
                    </div>
                    <button className="ghost" onClick={listInvitations} disabled={!token || busyAction === "邀请码列表"}>
                      <RefreshCcw size={17} />
                      刷新
                    </button>
                  </div>

                  <div className="invite-items">
                    {invitations.length === 0 ? (
                      <div className="empty-state">登录 OWNER 或 ADMIN 后刷新邀请码列表</div>
                    ) : (
                      invitations.map((invitation) => (
                        <article className="invite-item" key={invitation.id}>
                          <div>
                            <strong>{invitation.codePrefix}</strong>
                            <span>{invitation.type} · {invitation.boundRoles.join(" / ")}</span>
                          </div>
                          <div>
                            <b className={`status-pill ${invitation.status.toLowerCase()}`}>{invitation.status}</b>
                            <span>{invitation.usedCount}/{invitation.maxUses}</span>
                          </div>
                          <button className="icon-action" onClick={() => disableInvitation(invitation.id)} disabled={!token || invitation.status === "DISABLED" || busyAction === "禁用邀请码"}>
                            <Ban size={17} />
                          </button>
                        </article>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </section>
          </section>

          <aside className="panel response-panel" ref={logsRef}>
            <div className="panel-title">
              <Activity size={22} />
              <h2>最近事件</h2>
            </div>
            <div className="event-head">
              <span className={lastResponse?.code === 0 ? "event-ok" : "event-warn"}>
                {lastResponse?.code === 0 ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}
                {lastAction}
              </span>
              <small>{formatTime(new Date().toISOString())}</small>
            </div>
            <pre>{lastResponse ? JSON.stringify(lastResponse, null, 2) : "等待接口响应"}</pre>
          </aside>
        </section>
      </section>
    </main>
  );
}

function csvValues(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function InfoCard({ icon, label, value, hint }: { icon: ReactNode; label: string; value: string; hint: string }) {
  return (
    <article className="info-card">
      <div className="icon-box">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{hint}</small>
    </article>
  );
}

function Field({
  label,
  value,
  type = "text",
  onChange
}: {
  label: string;
  value: string;
  type?: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}
