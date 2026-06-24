/**
 * API 统一类型 — 响应结构、分页、错误
 * 供 api/client.ts、api/modules/*.ts、hooks/useRequest.ts 引用
 */

/** 后端统一响应外层，code === 0 表示业务成功 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId?: string
  errors?: FieldError[]
}

/** 分页响应，所有列表接口统一使用 */
export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

/** 字段校验错误，表单做字段级提示用 */
export interface FieldError {
  field: string
  reason: string
}

/** API client 请求可选参数 */
export interface RequestOptions {
  params?: Record<string, string | number | boolean | undefined>
  headers?: Record<string, string>
  signal?: AbortSignal
}

/** API client 捕获到的业务错误，页面按 code 分流处理 */
export class ApiError extends Error {
  code: number
  requestId?: string
  errors?: FieldError[]

  constructor(code: number, message: string, requestId?: string, errors?: FieldError[]) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.requestId = requestId
    this.errors = errors
  }
}
