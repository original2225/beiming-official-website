/**
 * 确认操作 hook — 后台/运维写操作统一确认流程
 * 页面调用 confirm() 弹出确认框，用户确认后返回 true，取消返回 false
 *
 * 使用方式：
 *   const { confirm, dialog } = useConfirmAction()
 *   const ok = await confirm({ title: '删除', message: '确定？', risk: 'HIGH' })
 *   if (ok) { ... 执行写操作 ... }
 *   组件底部放 {dialog}
 */

import { useState, useCallback } from 'react'
import type { RiskLevel } from '../types/common'
import { ConfirmDialog } from '../components/common/ConfirmDialog'

interface ConfirmConfig {
  title: string
  message: string
  risk: RiskLevel
  confirmText?: string
}

export function useConfirmAction() {
  const [open, setOpen] = useState(false)
  const [config, setConfig] = useState<ConfirmConfig | null>(null)
  const [resolver, setResolver] = useState<((ok: boolean) => void) | null>(null)

  const confirm = useCallback((cfg: ConfirmConfig): Promise<boolean> => {
    setConfig(cfg)
    setOpen(true)
    return new Promise<boolean>((resolve) => {
      setResolver(() => resolve)
    })
  }, [])

  const handleConfirm = useCallback(() => {
    setOpen(false)
    resolver?.(true)
  }, [resolver])

  const handleCancel = useCallback(() => {
    setOpen(false)
    resolver?.(false)
  }, [resolver])

  const dialog = config ? (
    <ConfirmDialog
      open={open}
      title={config.title}
      message={config.message}
      risk={config.risk}
      confirmText={config.confirmText}
      onConfirm={handleConfirm}
      onCancel={handleCancel}
    />
  ) : null

  return { confirm, dialog }
}
