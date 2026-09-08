import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { likePost, unlikePost } from './likes'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('likes api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
    vi.mocked(apiFetch).mockResolvedValue({ likeCount: 1, isLikedByMe: true })
  })

  it('likePost calls apiFetch with POST', async () => {
    await likePost(1)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1/likes', { method: 'POST' })
  })

  it('unlikePost calls apiFetch with DELETE', async () => {
    await unlikePost(1)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1/likes', { method: 'DELETE' })
  })
})
