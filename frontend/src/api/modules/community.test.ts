import { afterEach, describe, expect, it, vi } from 'vitest'
import { getBoards } from './community'

describe('community api adapters', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns board items from the backend page result for the current board list page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          items: [
            { boardId: 'board-1', name: '公告讨论', description: '讨论区', postCount: 3 },
          ],
          page: 1,
          pageSize: 20,
          total: 1,
        },
      }),
    }))

    await expect(getBoards()).resolves.toEqual([
      { boardId: 'board-1', name: '公告讨论', description: '讨论区', postCount: 3 },
    ])
  })
})
