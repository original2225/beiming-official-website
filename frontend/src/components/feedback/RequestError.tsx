/** 请求失败提示 — 公开页局部降级用，显示 requestId */
import { ApiError } from '../../types/api'

export function RequestError({ error }: { error: ApiError }) {
  return (
    <div className="text-center py-4 text-cinnabar text-sm">
      服务暂不可用
      {error.requestId && <span className="text-text-muted ml-2">({error.requestId})</span>}
    </div>
  )
}
