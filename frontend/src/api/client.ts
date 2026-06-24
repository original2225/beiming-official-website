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

  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(tokenGetter?.() ? { Authorization: `Bearer ${tokenGetter!()}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: abortSignal,
  })

  clearTimeout(timeoutId)

  const json: ApiResponse<T> = await res.json()

  if (json.code !== 0) {
    throw new ApiError(json.code, json.message, json.requestId, json.errors)
  }

  return json.data
}

// 便捷方法
export const get = <T>(path: string, options?: RequestOptions) => request<T>(path, 'GET', undefined, options)
export const post = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'POST', body, options)
export const put = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'PUT', body, options)
export const patch = <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>(path, 'PATCH', body, options)
export const del = <T>(path: string, options?: RequestOptions) => request<T>(path, 'DELETE', undefined, options)
