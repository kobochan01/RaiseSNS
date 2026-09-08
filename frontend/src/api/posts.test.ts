import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { createPost, deletePost, getTimeline, updatePost } from './posts'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('posts api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
    vi.mocked(apiFetch).mockResolvedValue({ posts: [], nextCursor: null })
  })

  it('getTimeline builds query with scope and limit', async () => {
    await getTimeline('all', undefined, 20)

    expect(apiFetch).toHaveBeenCalledWith('/posts?scope=all&limit=20')
  })

  it('getTimeline uses cursor param when provided', async () => {
    await getTimeline('all', 5, 20)

    expect(apiFetch).toHaveBeenCalledWith('/posts?scope=all&limit=20&cursor=5')
  })

  it('getTimeline uses sinceId param and ignores cursor when both provided', async () => {
    await getTimeline('following', 5, 20, 10)

    expect(apiFetch).toHaveBeenCalledWith('/posts?scope=following&limit=20&sinceId=10')
  })

  it('getTimeline omits cursor and sinceId when neither provided', async () => {
    await getTimeline('all')

    expect(apiFetch).toHaveBeenCalledWith('/posts?scope=all&limit=20')
  })

  it('createPost calls apiFetch with POST and correct body', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})
    const request = { body: 'こんにちは' }

    await createPost(request)

    expect(apiFetch).toHaveBeenCalledWith('/posts', { method: 'POST', body: JSON.stringify(request) })
  })

  it('updatePost calls apiFetch with PUT and correct path', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})
    const request = { body: '更新後' }

    await updatePost(1, request)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1', { method: 'PUT', body: JSON.stringify(request) })
  })

  it('deletePost calls apiFetch with DELETE and correct path', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await deletePost(1)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1', { method: 'DELETE' })
  })
})
