import { render as rtlRender, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createComment, getComments, type Comment } from '../api/comments'
import { likePost } from '../api/likes'
import type { Post } from '../api/posts'
import { PostDetailModal } from './PostDetailModal'

function render(ui: ReactElement) {
  return rtlRender(ui, { wrapper: MemoryRouter })
}

vi.mock('../api/comments', () => ({
  getComments: vi.fn(),
  createComment: vi.fn(),
  deleteComment: vi.fn(),
}))

vi.mock('../api/likes', () => ({
  likePost: vi.fn(),
  unlikePost: vi.fn(),
}))

function makePost(overrides: Partial<Post> = {}): Post {
  return {
    id: 1,
    author: { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null },
    body: '今日はいい天気ですね',
    imageUrl: null,
    likeCount: 3,
    commentCount: 1,
    isLikedByMe: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  }
}

function makeComment(overrides: Partial<Comment> = {}): Comment {
  return {
    id: 1,
    author: { id: 2, username: 'hanako', displayName: '花子', avatarUrl: null },
    body: 'いいですね！',
    parentCommentId: null,
    createdAt: new Date().toISOString(),
    replies: [],
    ...overrides,
  }
}

describe('PostDetailModal', () => {
  const onClose = vi.fn()
  const onUpdated = vi.fn()

  beforeEach(() => {
    onClose.mockClear()
    onUpdated.mockClear()
    vi.mocked(getComments).mockReset()
    vi.mocked(createComment).mockReset()
    vi.mocked(likePost).mockReset()
  })

  it('loads and renders comments for the post', async () => {
    vi.mocked(getComments).mockResolvedValue([makeComment()])

    render(<PostDetailModal post={makePost()} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)

    await waitFor(() => expect(getComments).toHaveBeenCalledWith(1))
    expect(await screen.findByText('いいですね！')).toBeInTheDocument()
  })

  it('shows the empty state when there are no comments', async () => {
    vi.mocked(getComments).mockResolvedValue([])

    render(<PostDetailModal post={makePost()} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)

    expect(await screen.findByText('まだコメントがありません。')).toBeInTheDocument()
  })

  it('renders the post body and like button', async () => {
    vi.mocked(getComments).mockResolvedValue([])

    render(<PostDetailModal post={makePost()} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)

    expect(screen.getByText('今日はいい天気ですね')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('calls onClose when the close button is clicked', async () => {
    vi.mocked(getComments).mockResolvedValue([])

    render(<PostDetailModal post={makePost()} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)
    await screen.findByText('まだコメントがありません。')

    await userEvent.click(screen.getByRole('button', { name: '閉じる' }))

    expect(onClose).toHaveBeenCalled()
  })

  it('submits a new top-level comment and updates the comment count', async () => {
    vi.mocked(getComments).mockResolvedValue([])
    const created = makeComment({ id: 5, body: '新しいコメント' })
    vi.mocked(createComment).mockResolvedValue(created)
    const post = makePost({ commentCount: 1 })

    render(<PostDetailModal post={post} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)
    await screen.findByText('まだコメントがありません。')

    await userEvent.type(screen.getByPlaceholderText('コメントを入力...(1〜140文字)'), '新しいコメント')
    await userEvent.click(screen.getByRole('button', { name: '送信' }))

    await waitFor(() => expect(createComment).toHaveBeenCalledWith(1, '新しいコメント'))
    expect(await screen.findByText('新しいコメント')).toBeInTheDocument()
    await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ commentCount: 2 })))
  })

  it('toggles the like and calls onUpdated with the new state', async () => {
    vi.mocked(getComments).mockResolvedValue([])
    vi.mocked(likePost).mockResolvedValue({ likeCount: 4, isLikedByMe: true })

    render(<PostDetailModal post={makePost()} currentUserId={1} onClose={onClose} onUpdated={onUpdated} />)
    await screen.findByText('まだコメントがありません。')

    await userEvent.click(screen.getByRole('button', { name: /♡/ }))

    await waitFor(() =>
      expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ likeCount: 4, isLikedByMe: true })),
    )
  })
})
