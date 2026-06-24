/**
 * API 常量 — 网关地址、模块端口、超时
 * 供 api/client.ts 和需要直连排障的调试页引用
 */

/** 默认网关地址，本地联调走这里，可通过 VITE_API_BASE_URL 覆盖 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8135'

/** 请求默认超时 ms */
export const API_TIMEOUT = 15000

/** 各模块直连端口，仅排障用，业务页面不要写死单服务端口 */
export const SERVICE_PORTS: Record<string, number> = {
  auth: 8101,
  profile: 8102,
  notification: 8103,
  content: 8104,
  'server-status': 8105,
  resource: 8106,
  admin: 8107,
  onboarding: 8108,
  exam: 8109,
  whitelist: 8110,
  attendance: 8111,
  community: 8112,
  activity: 8113,
  calendar: 8114,
  changelog: 8115,
  'ops-control': 8116,
  'node-daemon': 8117,
  'cloudreve-sync': 8118,
  'backup-recovery': 8119,
  alerting: 8120,
  'online-map': 8121,
  'plugin-integration': 8122,
  'cross-platform-notification': 8123,
  'ops-image-market': 8124,
  'api-gateway': 8135,
  material: 8126,
  guide: 8127,
}
