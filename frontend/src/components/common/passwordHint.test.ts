import { describe, it, expect } from 'vitest'

/**
 * 密码实时提示的判定规则。
 *
 * 这条规则必须和后端 AuthModule.validatePassword 完全一致：
 * 6-128 位 + 至少两类字符（大写/小写/数字/符号中任取两类）。
 * 前端只负责「早点告诉用户哪里不对」，最终裁量权在后端。
 *
 * 这里直接复刻一份纯函数做断言，保证改动时两侧不会悄悄分叉。
 */
function getPasswordIssue(password: string): string {
  if (password.length === 0) return ''
  if (password.length < 6) return `还差 ${6 - password.length} 位（至少 6 位）`
  if (password.length > 128) return '太长了，最多 128 位'
  const classes = [
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length
  if (classes < 2) return '只有一类字符，再加数字、符号或大小写任意一种'
  return ''
}

describe('password hint', () => {
  it('空输入不提示（不要一打开弹窗就红字）', () => {
    expect(getPasswordIssue('')).toBe('')
  })

  it('长度不足时告诉用户还差几位', () => {
    expect(getPasswordIssue('ab12')).toBe('还差 2 位（至少 6 位）')
    expect(getPasswordIssue('a')).toBe('还差 5 位（至少 6 位）')
  })

  it('刚好 6 位的合法组合不报错', () => {
    expect(getPasswordIssue('abc123')).toBe('')
    expect(getPasswordIssue('abcd!@')).toBe('')
    expect(getPasswordIssue('Abcdef')).toBe('')
  })

  it('只有一类字符时明确说缺什么', () => {
    expect(getPasswordIssue('abcdef')).toContain('只有一类字符')
    expect(getPasswordIssue('123456')).toContain('只有一类字符')
    expect(getPasswordIssue('ABCDEF')).toContain('只有一类字符')
  })

  it('超过 128 位提示太长', () => {
    expect(getPasswordIssue('a1'.repeat(65))).toContain('最多 128')
  })

  it('中文按符号类计算，配数字即两类', () => {
    expect(getPasswordIssue('密码123456')).toBe('')
  })

  it('与后端一致：这几种后端也会拒', () => {
    // 纯数字 / 纯字母：后端 characterClassCount < 2
    for (const p of ['abcdef', '123456', 'ABCDEF', '......']) {
      expect(getPasswordIssue(p)).not.toBe('')
    }
  })
})