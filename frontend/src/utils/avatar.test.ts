import { describe, expect, it } from 'vitest'
import { avatarColor } from './avatar'

describe('avatarColor', () => {
  it('returns the same color for the same numeric seed', () => {
    expect(avatarColor(3)).toBe(avatarColor(3))
  })

  it('wraps numeric seed using modulo of palette size', () => {
    expect(avatarColor(0)).toBe(avatarColor(8))
  })

  it('returns the same color for the same string seed (deterministic hashing)', () => {
    expect(avatarColor('taro')).toBe(avatarColor('taro'))
  })

  it('returns a valid color for an empty string seed', () => {
    expect(() => avatarColor('')).not.toThrow()
    expect(avatarColor('')).toEqual(expect.any(String))
  })
})
