import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { getProfile, getUserPosts, updateProfile } from './users'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('users api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
  })

  it('getProfile calls apiFetch with correct path', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})

    await getProfile('taro')

    expect(apiFetch).toHaveBeenCalledWith('/users/taro')
  })

  it('updateProfile calls apiFetch with PUT and correct path', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})
    const request = { displayName: '太郎', bio: null }

    await updateProfile('taro', request)

    expect(apiFetch).toHaveBeenCalledWith('/users/taro', { method: 'PUT', body: JSON.stringify(request) })
  })

  it('getUserPosts omits query string when cursor is undefined', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ posts: [], nextCursor: null })

    await getUserPosts('taro')

    expect(apiFetch).toHaveBeenCalledWith('/users/taro/posts')
  })

  it('getUserPosts includes cursor query param when provided', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ posts: [], nextCursor: null })

    await getUserPosts('taro', 5)

    expect(apiFetch).toHaveBeenCalledWith('/users/taro/posts?cursor=5')
  })
})
