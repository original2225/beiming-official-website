/**
 * 登录页 — ProtectedRoute 未登录时的落地页
 * 复用 LoginModal / RegisterModal，登录成功后回跳来源页
 */

import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { LoginModal } from '../../components/common/LoginModal'
import { RegisterModal } from '../../components/common/RegisterModal'
import { useAuthStore } from '../../store/authStore'
import { ROUTES } from '../../constants/routes'

interface LoginLocationState {
  from?: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
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
      <LoginModal
        open={mode === 'login'}
        onClose={close}
        onSwitchToRegister={() => setMode('register')}
      />
      <RegisterModal
        open={mode === 'register'}
        onClose={close}
        onSwitchToLogin={() => setMode('login')}
      />
    </div>
  )
}