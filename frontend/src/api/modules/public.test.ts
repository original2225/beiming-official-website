import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  getGuideDetail,
  getGuides,
  getResourceDetail,
  getResources,
  getServerLines,
  getServerOverview,
} from './public'

describe('public api adapters', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns server line items from the backend page result for the current status page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          items: [
            { lineId: 'line-1', name: '主线路', address: 'play.example.test', latency: 42, online: true },
          ],
          page: 1,
          pageSize: 20,
          total: 1,
        },
      }),
    }))

    await expect(getServerLines()).resolves.toEqual([
      { lineId: 'line-1', name: '主线路', address: 'play.example.test', latency: 42, online: true },
    ])
  })

  it('maps the backend server overview fields to the status view model', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          overallStatus: 'ONLINE',
          onlinePlayers: 12,
          maxPlayers: 80,
          version: '1.21.1',
          motd: 'Beiming',
          lines: [
            { lineId: 'line-1', name: '主线路', entryAddress: 'play.example.test', status: 'AVAILABLE', latencyMs: 42 },
          ],
        },
      }),
    }))

    await expect(getServerOverview()).resolves.toEqual({
      online: true,
      playerCount: 12,
      maxPlayers: 80,
      version: '1.21.1',
      motd: 'Beiming',
      lines: [
        { lineId: 'line-1', name: '主线路', address: 'play.example.test', latency: 42, online: true },
      ],
    })
  })

  it('maps backend server line field names from the page result', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          items: [
            { lineId: 'line-1', name: '主线路', entryAddress: 'play.example.test', status: 'AVAILABLE', latencyMs: 42 },
            { lineId: 'line-2', name: '备用线路', entryAddress: 'backup.example.test', status: 'UNAVAILABLE', latencyMs: null },
          ],
          page: 1,
          pageSize: 20,
          total: 2,
        },
      }),
    }))

    await expect(getServerLines()).resolves.toEqual([
      { lineId: 'line-1', name: '主线路', address: 'play.example.test', latency: 42, online: true },
      { lineId: 'line-2', name: '备用线路', address: 'backup.example.test', latency: 0, online: false },
    ])
  })

  it('maps backend resource category and latest version fields for list pages', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          items: [
            {
              resourceId: 'res-1',
              title: '材质包',
              slug: 'pack',
              category: { categoryId: 'cat-1', name: '资源包' },
              latestVersion: { versionName: '1.0.0', downloadCount: 7 },
              coverUrl: '/cover.png',
            },
          ],
          page: 1,
          pageSize: 20,
          total: 1,
        },
      }),
    }))

    await expect(getResources()).resolves.toEqual({
      items: [
        {
          resourceId: 'res-1',
          title: '材质包',
          slug: 'pack',
          category: '资源包',
          version: '1.0.0',
          downloads: 7,
          coverUrl: '/cover.png',
          latestVersion: { versionName: '1.0.0', downloadCount: 7 },
        },
      ],
      page: 1,
      pageSize: 20,
      total: 1,
    })
  })

  it('maps backend resource detail fields for the detail page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      json: async () => ({
        code: 0,
        message: 'success',
        data: {
          resourceId: 'res-1',
          title: '材质包',
          category: { categoryId: 'cat-1', name: '资源包' },
          latestVersion: { versionName: '1.0.0', downloadUrl: 'https://example.test/download' },
          description: '适配说明',
        },
      }),
    }))

    await expect(getResourceDetail('res-1')).resolves.toMatchObject({
      title: '材质包',
      category: '资源包',
      version: '1.0.0',
      downloadUrl: 'https://example.test/download',
      description: '适配说明',
    })
  })

  it('maps backend guide category objects for list and detail pages', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce({
        json: async () => ({
          code: 0,
          message: 'success',
          data: {
            items: [
              {
                guideId: 'guide-1',
                title: '入服指南',
                slug: 'join',
                category: { categoryId: 'cat-1', name: '新手' },
                updatedAt: '2026-06-01T00:00:00Z',
              },
            ],
            page: 1,
            pageSize: 20,
            total: 1,
          },
        }),
      })
      .mockResolvedValueOnce({
        json: async () => ({
          code: 0,
          message: 'success',
          data: {
            guideId: 'guide-1',
            title: '入服指南',
            category: { categoryId: 'cat-1', name: '新手' },
            updatedAt: '2026-06-01T00:00:00Z',
            body: '内容',
          },
        }),
      }))

    await expect(getGuides()).resolves.toMatchObject({
      items: [
        {
          guideId: 'guide-1',
          title: '入服指南',
          slug: 'join',
          category: '新手',
          updatedAt: '2026-06-01T00:00:00Z',
        },
      ],
    })
    await expect(getGuideDetail('guide-1')).resolves.toMatchObject({
      title: '入服指南',
      category: '新手',
      updatedAt: '2026-06-01T00:00:00Z',
      body: '内容',
    })
  })
})
