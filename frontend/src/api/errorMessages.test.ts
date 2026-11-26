import { describe, it, expect } from 'vitest'
import { describeApiError } from './errorMessages'

/**
 * 错误码翻译的回归测试。
 *
 * 背景：2026-11-26 花蝉验收时点注册，界面只弹「注册失败，请稍后重试」，
 * 而真实原因是邀请码用尽（43114）。一句兜底文案等于没给失败原因。
 * 这些用例锁住「常见错误码必须翻译成具体中文」这件事。
 */
describe('describeApiError', () => {
  it('把邀请码相关的错误码翻译成具体原因', () => {
    expect(describeApiError(43114)).toContain('邀请码已用尽')
    expect(describeApiError(43101)).toContain('邀请码不存在')
    expect(describeApiError(43112)).toContain('已被禁用')
    expect(describeApiError(43113)).toContain('已过期')
  })

  it('把账号冲突翻译成具体原因', () => {
    expect(describeApiError(43110)).toContain('用户名已被注册')
    expect(describeApiError(43111)).toContain('显示名已被使用')
  })

  it('把登录类错误翻译成具体原因', () => {
    expect(describeApiError(41100)).toContain('用户名或密码错误')
    expect(describeApiError(41102)).toContain('封禁')
    expect(describeApiError(44100)).toContain('过于频繁')
  })

  it('code 0 是前端合成的错误，直接用自带的 message', () => {
    const msg = '无法连接到服务器（http://127.0.0.1:8135）'
    expect(describeApiError(0, msg)).toBe(msg)
  })

  it('未收录的错误码退化为后端原文加错误码，不吞掉线索', () => {
    // 宁可显示英文 + 码，也不要显示一句无信息量的兜底
    expect(describeApiError(99999, 'something odd')).toBe('something odd（错误码 99999）')
  })

  it('未收录且没有 message 时仍然带出错误码', () => {
    expect(describeApiError(99999)).toBe('请求失败（错误码 99999）')
  })

  it('任何错误码都不会返回空字符串', () => {
    for (const code of [0, 40001, 43114, 41100, 99999]) {
      expect(describeApiError(code, 'x').length).toBeGreaterThan(0)
    }
  })
})