/**
 * API 客户端 — 统一请求、自动挂认证头、响应解析、错误抛出
 * 供 api/modules/*.ts 和 hooks/useRequest.ts 引用
 */

import type { ApiResponse, RequestOptions } from '../types/api'
import { ApiError } from '../types/api'
import { API_BASE_URL, API_TIMEOUT } from '../constants/api'

let tokenGetter: (() => string | null) | null = null

/** 注册 token 获取函数，拉起 app 时由 authStore 调用 */
export function setTokenGetter(fn: () => string | null) {
  tokenGetter = fn
}

/** 构建完整请求 URL */
export function buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
  const url = new URL(path, API_BASE_URL)
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined) url.searchParams.set(k, String(v))
    })
  }
  return url.toString()
}

/** 统一请求，自动解析 ApiResponse，code !== 0 抛出 ApiError */
export async function request<T>(
  path: string,
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' = 'GET',
  body?: unknown,
  options: RequestOptions = {},
): Promise<T> {
  const { params, headers, signal } = options

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT)
  const abortSignal = signal ?? controller.signal

  const url = buildUrl(path, params)

  let res: Response
  try {
    res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(tokenGetter?.() ? { Authorization: `Bearer ${tokenGetter!()}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: abortSignal,
    })
  } catch (e) {
    clearTimeout(timeoutId)
    // fetch 只在「请求根本没发出去」时抛 TypeError：网络断了、跨域被 CORS 拦、
    // 后端没起、DNS 解析失败。它拿不到状态码也拿不到响应体，如果原样往上抛，
    // 页面只能显示一句兜底文案，用户和开发者都看不出到底哪里错了。
    // 所以这里一律翻译成带原因的 ApiError。
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw new ApiError(0, '请求超时，请检查网络后重试')
    }
    throw new ApiError(
      0,
      `无法连接到服务器（${API_BASE_URL}）。可能是后端未启动、网络中断，或浏览器拦截了跨域请求。`,
    )
  }

  clearTimeout(timeoutId)

  // 非 2xx 也要读响应体：后端在 4xx/5xx 时同样返回 { code, message }，
  // 直接把状态码丢掉会让「邀请码已用尽」这类具体原因变成一句「请求失败」。
  let json: ApiResponse<T> | null = null
  try {
    json = (await res.json()) as ApiResponse<T>
  } catch {
    json = null
  }

  if (json && typeof json.code === 'number') {
    if (json.code !== 0) {
      throw new ApiError(json.code, json.message || `请求失败（HTTP ${res.status}）`, json.requestId, json.errors)
    }
    return json.data
  }

  // 响应体不是约定的 { code, message } 结构（网关 502 HTML 页、静态服务兜底页等）
  throw new ApiError(0, `服务器返回异常响应（HTTP ${res.status}），请稍后重试`)
}

// 便捷方法
export const get = <T>(path: string, options?: RequestOptions) => request<T>(path, 'GET', undefined, options)
export const post = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'POST', body, options)
export const put = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'PUT', body, options)
export const patch = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'PATCH', body, options)
export const del = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'DELETE', body, options)
