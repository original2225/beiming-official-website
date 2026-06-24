import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../store/authStore'
import { ProtectedRoute } from './ProtectedRoute'

function renderProtected() {
  render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route path="/login" element={<div>login page</div>} />
        <Route
          path="/private"
          element={(
            <ProtectedRoute>
              <div>private page</div>
            </ProtectedRoute>
          )}
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  afterEach(() => {
    useAuthStore.getState().clearAuth()
  })

  it('redirects unauthenticated users to login in development too', () => {
    renderProtected()

    expect(screen.getByText('login page')).toBeInTheDocument()
    expect(screen.queryByText('private page')).not.toBeInTheDocument()
  })
})
