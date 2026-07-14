/**
 * 公开页顶部导航
 */

import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ROUTES } from '../../constants/routes'
import { useAuthStore } from '../../store/authStore'
import { LoginModal } from '../common/LoginModal'
import { RegisterModal } from '../common/RegisterModal'

const NAV_ITEMS = [
  { label: '首页', to: ROUTES.HOME },
  { label: '公告', to: ROUTES.ANNOUNCEMENTS },
  { label: '指南', to: ROUTES.GUIDES },
  { label: '资源', to: ROUTES.RESOURCES },
  { label: '状态', to: ROUTES.SERVER_STATUS },
  { label: '成员', to: ROUTES.MEMBERS },
  { label: '素材', to: ROUTES.MATERIALS },
  { label: '活动', to: ROUTES.ACTIVITIES },
  { label: '日历', to: ROUTES.CALENDAR },
  { label: '更新', to: ROUTES.CHANGELOG },
  { label: '社区', to: ROUTES.BOARDS },
]

export function PublicNav() {
  const location = useLocation()
  const { isAuthenticated, user } = useAuthStore()
  const [loginOpen, setLoginOpen] = useState(false)
  const [registerOpen, setRegisterOpen] = useState(false)

  const openRegister = () => { setLoginOpen(false); setRegisterOpen(true) }
  const openLogin = () => { setRegisterOpen(false); setLoginOpen(true) }

  const isActive = (to: string) => {
    if (location.pathname === to) return true
    if (to === ROUTES.BOARDS && location.pathname.startsWith('/community')) return true
    return false
  }

  return (
    <>
      <nav className="bg-surface border-b border-ink-600/30">
        <div className="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
          <Link to={ROUTES.HOME} className="font-display text-indigo text-xl tracking-wider">
            北冥
          </Link>

          <div className="flex items-center gap-1">
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.to)
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`px-3 py-1 text-sm transition-colors rounded-md
                    ${active
                      ? 'text-ochre border-b-2 border-indigo'
                      : 'text-text-secondary hover:text-text-primary'
                    }`}
                >
                  {item.label}
                </Link>
              )
            })}

            <div className="ml-3 pl-3 border-l border-ink-600/30">
              {isAuthenticated ? (
                <Link to={ROUTES.PROFILE} className="px-3 py-1 text-sm text-ochre hover:text-indigo">
                  {user?.username ?? '用户中心'}
                </Link>
              ) : (
                <button onClick={() => setLoginOpen(true)} className="px-3 py-1 text-sm text-indigo hover:text-ochre">
                  登录
                </button>
              )}
            </div>
          </div>
        </div>
      </nav>

      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} onSwitchToRegister={openRegister} />
      <RegisterModal open={registerOpen} onClose={() => setRegisterOpen(false)} onSwitchToLogin={openLogin} />
    </>
  )
}
