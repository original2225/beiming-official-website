export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
  requestId?: string;
  errors?: Array<{ field: string; reason: string }>;
};

export type UserRole = "OWNER" | "ADMIN" | "HELPER" | "USER";

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roles: UserRole[];
  permissions: string[];
  status: string;
};

export type DashboardData = {
  contentTitle: string;
  serverStatus: string;
  onlinePlayers: number;
  resourcesTotal: number;
  guidesTotal: number;
  activitiesTotal: number;
  changelogTitle: string;
  degradedModules: string[];
  requestId: string;
};

export type AdminData = {
  modules: string[];
  todos: number;
  audits: number;
  platformDependencies: string[];
  degradedModules: string[];
  requestId: string;
};

export type OpsData = {
  mode: string;
  nodes: number;
  assets: number;
  tasks: number;
  approvals: number;
  requestId: string;
};

type DataAdapter<T> = (data: unknown, fallback: T) => T;

const env = import.meta.env as ImportMetaEnv & {
  readonly VITE_BEIMING_API_BASE?: string;
  readonly VITE_BEIMING_API_MODE?: string;
};

export const API_BASE =
  env.VITE_BEIMING_API_MODE === "direct"
    ? env.VITE_BEIMING_API_BASE ?? "http://127.0.0.1:8125"
    : env.VITE_BEIMING_API_BASE ?? "http://127.0.0.1:8125";

const requestId = "req_mock";

export function currentMockMode() {
  return new URLSearchParams(window.location.search).get("mock");
}

export async function requestEnvelope<T>(path: string, fallback: T, adapter?: DataAdapter<T>): Promise<ApiEnvelope<T>> {
  if (currentMockMode()) {
    return mockEnvelope(path, fallback);
  }
  const currentRequestId = `req_${Date.now()}`;
  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers: {
        "Content-Type": "application/json",
        "X-Request-Id": currentRequestId
      }
    });
    const envelope = (await response.json()) as ApiEnvelope<unknown>;
    if (!response.ok || envelope.code !== 0) {
      return {
        code: envelope.code || response.status,
        message: envelope.message || "request failed",
        requestId: envelope.requestId || currentRequestId,
        data: degradeFallback(path, fallback)
      };
    }
    return {
      ...envelope,
      data: adapter ? adapter(envelope.data, fallback) : (envelope.data as T)
    };
  } catch {
    return {
      code: 503,
      message: "local degraded fallback",
      requestId: currentRequestId,
      data: degradeFallback(path, fallback)
    };
  }
}

function mockEnvelope<T>(path: string, fallback: T): ApiEnvelope<T> {
  const mode = currentMockMode();
  if (mode === "degraded" && path.startsWith("/api/v1/content/home")) {
    return {
      code: 0,
      message: "success",
      requestId,
      data: {
        contentTitle: "北冥项目门户",
        serverStatus: "DEGRADED",
        onlinePlayers: 0,
        resourcesTotal: 0,
        guidesTotal: 0,
        activitiesTotal: 0,
        changelogTitle: "服务状态暂不可用",
        degradedModules: ["CONTENT", "SERVER_STATUS", "RESOURCE"],
        requestId
      } as T
    };
  }
  if (path.startsWith("/api/v1/admin/overview")) {
    return {
      code: 0,
      message: "success",
      requestId,
      data: {
        modules: ["AUTH", "PROFILE", "CONTENT", "RESOURCE", "GUIDE", "OPS_CONTROL"],
        todos: 18,
        audits: 42,
        platformDependencies: ["API_GATEWAY"],
        degradedModules: mode === "admin" ? ["RESOURCE"] : [],
        requestId
      } as T
    };
  }
  if (path.startsWith("/api/v1/ops-control/overview")) {
    return {
      code: 0,
      message: "success",
      requestId,
      data: {
        mode: "SIMULATED",
        nodes: 3,
        assets: 21,
        tasks: 7,
        approvals: 2,
        requestId
      } as T
    };
  }
  if (path.startsWith("/api/v1/auth/session/verify")) {
    return {
      code: 0,
      message: "success",
      requestId,
      data: {
        id: "user_owner",
        username: "owner",
        displayName: "北冥管理者",
        roles: ["OWNER"],
        permissions: ["NODE_READ", "HIGH_RISK_APPROVE"],
        status: "ACTIVE"
      } as T
    };
  }
  return {
    code: 0,
    message: "success",
    requestId,
    data: fallback
  };
}

function degradeFallback<T>(path: string, fallback: T): T {
  if (!fallback || typeof fallback !== "object" || !("degradedModules" in fallback)) {
    return fallback;
  }
  const moduleKey = moduleFromPath(path);
  const data = fallback as T & { degradedModules?: string[]; requestId?: string };
  return {
    ...data,
    degradedModules: moduleKey ? Array.from(new Set([...(data.degradedModules ?? []), moduleKey])) : data.degradedModules,
    requestId
  };
}

function moduleFromPath(path: string) {
  if (path.includes("/content/")) return "CONTENT";
  if (path.includes("/server-status/")) return "SERVER_STATUS";
  if (path.includes("/resources")) return "RESOURCE";
  if (path.includes("/guides")) return "GUIDE";
  if (path.includes("/admin/")) return "ADMIN";
  if (path.includes("/ops-control/")) return "OPS_CONTROL";
  return null;
}
