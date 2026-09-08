import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatRelativeTime } from './time'

const NOW = new Date('2026-01-10T12:00:00')

function isoBefore(ms: number): string {
  return new Date(NOW.getTime() - ms).toISOString()
}

describe('formatRelativeTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns たった今 for less than 60 seconds', () => {
    expect(formatRelativeTime(isoBefore(30 * 1000))).toBe('たった今')
  })

  it('returns n分前 for less than 60 minutes', () => {
    expect(formatRelativeTime(isoBefore(5 * 60 * 1000))).toBe('5分前')
  })

  it('returns n時間前 for less than 24 hours', () => {
    expect(formatRelativeTime(isoBefore(3 * 60 * 60 * 1000))).toBe('3時間前')
  })

  it('returns n日前 for less than 7 days', () => {
    expect(formatRelativeTime(isoBefore(2 * 24 * 60 * 60 * 1000))).toBe('2日前')
  })

  it('returns YYYY/M/D for 7 days or more', () => {
    const target = isoBefore(10 * 24 * 60 * 60 * 1000)
    const d = new Date(target)

    expect(formatRelativeTime(target)).toBe(`${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`)
  })
})
