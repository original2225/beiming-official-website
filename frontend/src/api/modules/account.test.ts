import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../types/api'
import {
  bindMinecraft,
  changePassword,
  getMe,
  getUnreadCount,
  login,
  register,
  unbindMinecraft,
} from './account'

describe('account api adapters', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('maps the backend auth session payload to the frontend login shape', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          accessToken: 'ses_123',
          tokenType: 'Bearer',
          expiresAt: '2026-06-24T12:00:00Z',
          user: {
            id: 'usr_1',
            username: 'steve',
            displayName: 'Steve',
            roles: ['ADMIN', 'USER'],
            permissions: ['NODE_READ'],
            status: 'ACTIVE',
            minecraftBinding: {
              minecraftId: 'SteveMC',
              minecraftUuid: '0123456789abcdef0123456789abcdef',
            },
          },
        },
      }),
    }))

    await expect(login({ username: 'steve', password: 'secret123' })).resolves.toEqual({
      token: 'ses_123',
      user: {
        userId: 'usr_1',
        username: 'steve',
        role: 'ADMIN',
        permissions: ['NODE_READ'],
        minecraftName: 'SteveMC',
        minecraftUuid: '0123456789abcdef0123456789abcdef',
      },
    })
  })

  it('maps the backend current user summary to CurrentUser', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          id: 'usr_2',
          username: 'alex',
          displayName: 'Alex',
          roles: ['HELPER', 'USER'],
          permissions: [],
          status: 'ACTIVE',
          minecraftBinding: null,
        },
      }),
    }))

    await expect(getMe()).resolves.toMatchObject({
      userId: 'usr_2',
      username: 'alex',
      role: 'HELPER',
      permissions: [],
    })
  })

  it('fills displayName for registration when the existing form only provides username', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: async () => ({ code: 0, message: 'success', data: {} }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await register({ username: 'steve', password: 'secret123', invitationCode: 'INVITE' })

    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      username: 'steve',
      displayName: 'steve',
      password: 'secret123',
      invitationCode: 'INVITE',
    })
  })

  it('adapts password changes to the backend contract', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: async () => ({ code: 0, message: 'success', data: null }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await changePassword({ oldPassword: 'old-secret', newPassword: 'new-secret' })

    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      currentPassword: 'old-secret',
      newPassword: 'new-secret',
      reason: 'USER_PASSWORD_CHANGE',
    })
  })

  it('sends the required reason body when unbinding Minecraft', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: async () => ({ code: 0, message: 'success', data: null }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await unbindMinecraft()

    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'DELETE' })
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      reason: 'USER_MINECRAFT_UNBIND',
    })
  })

  it('does not send an incomplete Minecraft binding request from the current form', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(bindMinecraft({ minecraftName: 'SteveMC' })).rejects.toMatchObject({
      code: 40001,
    } satisfies Partial<ApiError>)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('keeps the backend unreadCount field name', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: { unreadCount: 7 },
      }),
    }))

    await expect(getUnreadCount()).resolves.toEqual({ unreadCount: 7 })
  })
})
