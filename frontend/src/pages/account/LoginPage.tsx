/**
 * 登录页 — ProtectedRoute 未登录时的落地页
 * 复用 LoginModal / RegisterModal，登录成功后回跳来源页
 */

import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { AuthBook } from '../../components/auth/AuthBook'
import { useAuthStore } from '../../store/authStore'
import { ROUTES } from '../../constants/routes'

interface LoginLocationState {
  from?: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  // 登录 / 注册是同一本活页册子的两页（花蝉 2026-11-28 定的）
  const [mode, setMode] = useState<'login' | 'register'>('login')

  const from = (location.state as LoginLocationState | null)?.from ?? ROUTES.HOME

  // 登录成功后回跳来源页；已在登录态直接访问 /login 也回跳
  useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true })
    }
  }, [isAuthenticated, from, navigate])

  const close = () => navigate(from, { replace: true })

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center">
      <AuthBook
        open
        page={mode}
        onClose={close}
        onSwitchToPage={setMode}
      />
    </div>
  )
}