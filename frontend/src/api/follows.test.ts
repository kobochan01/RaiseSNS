import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { followUser, getFollowers, getFollowing, unfollowUser } from './follows'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('follows api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
  })

  it('followUser calls apiFetch with POST', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ followerCount: 1, isFollowedByMe: true })

    await followUser('jiro')

    expect(apiFetch).toHaveBeenCalledWith('/users/jiro/follow', { method: 'POST' })
  })

  it('unfollowUser calls apiFetch with DELETE', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ followerCount: 0, isFollowedByMe: false })

    await unfollowUser('jiro')

    expect(apiFetch).toHaveBeenCalledWith('/users/jiro/follow', { method: 'DELETE' })
  })

  it('getFollowing extracts users array from response', async () => {
    const user = { id: 1, username: 'jiro', displayName: '次郎', avatarUrl: null, isFollowedByMe: true }
    vi.mocked(apiFetch).mockResolvedValue({ users: [user] })

    const result = await getFollowing('taro')

    expect(apiFetch).toHaveBeenCalledWith('/users/taro/following')
    expect(result).toEqual([user])
  })

  it('getFollowers extracts users array from response', async () => {
    const user = { id: 2, username: 'saburo', displayName: '三郎', avatarUrl: null, isFollowedByMe: false }
    vi.mocked(apiFetch).mockResolvedValue({ users: [user] })

    const result = await getFollowers('taro')

    expect(apiFetch).toHaveBeenCalledWith('/users/taro/followers')
    expect(result).toEqual([user])
  })
})
