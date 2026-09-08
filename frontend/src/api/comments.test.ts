import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { createComment, deleteComment, getComments } from './comments'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('comments api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
  })

  it('getComments calls apiFetch with GET on posts/:id/comments', async () => {
    vi.mocked(apiFetch).mockResolvedValue([])

    await getComments(1)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1/comments')
  })

  it('createComment sends body and parentCommentId', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})

    await createComment(1, 'コメント本文', 5)

    expect(apiFetch).toHaveBeenCalledWith('/posts/1/comments', {
      method: 'POST',
      body: JSON.stringify({ body: 'コメント本文', parentCommentId: 5 }),
    })
  })

  it('createComment defaults parentCommentId to null when omitted', async () => {
    vi.mocked(apiFetch).mockResolvedValue({})

    await createComment(1, 'コメント本文')

    expect(apiFetch).toHaveBeenCalledWith('/posts/1/comments', {
      method: 'POST',
      body: JSON.stringify({ body: 'コメント本文', parentCommentId: null }),
    })
  })

  it('deleteComment calls apiFetch with DELETE on comments/:id', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ commentCount: 0 })

    await deleteComment(9)

    expect(apiFetch).toHaveBeenCalledWith('/comments/9', { method: 'DELETE' })
  })
})
