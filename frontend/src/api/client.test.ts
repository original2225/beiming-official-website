import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../types/api'
import { API_BASE_URL } from '../constants/api'
import { buildUrl, request, setTokenGetter } from './client'

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    setTokenGetter(() => null)
  })

  it('uses the unified backend entrypoint by default', () => {
    expect(API_BASE_URL).toBe('http://127.0.0.1:8135')
    expect(buildUrl('/api/v1/auth/me')).toBe('http://127.0.0.1:8135/api/v1/auth/me')
  })

  it('throws an ApiError when the backend returns a business error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 41000,
        message: 'unauthenticated',
        data: null,
        requestId: 'req-test',
        errors: [{ field: 'Authorization', reason: 'required' }],
      }),
    }))

    await expect(request('/api/v1/auth/me')).rejects.toMatchObject({
      code: 41000,
      message: 'unauthenticated',
      requestId: 'req-test',
    } satisfies Partial<ApiError>)
  })

  it('attaches the bearer token from the registered token getter', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: async () => ({ code: 0, message: 'success', data: { ok: true } }),
    })
    vi.stubGlobal('fetch', fetchMock)
    setTokenGetter(() => 'token-123')

    await request('/api/v1/auth/me')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://127.0.0.1:8135/api/v1/auth/me',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer token-123' }),
      }),
    )
  })
})
