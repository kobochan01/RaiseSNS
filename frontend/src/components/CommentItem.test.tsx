import { render as rtlRender, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createComment, deleteComment, type Comment } from '../api/comments'
import { CommentItem } from './CommentItem'

function render(ui: ReactElement) {
  return rtlRender(ui, { wrapper: MemoryRouter })
}

vi.mock('../api/comments', () => ({
  createComment: vi.fn(),
  deleteComment: vi.fn(),
}))

function makeComment(overrides: Partial<Comment> = {}): Comment {
  return {
    id: 1,
    author: { id: 10, username: 'hanako', displayName: '花子', avatarUrl: null },
    body: 'いいですね',
    parentCommentId: null,
    createdAt: new Date().toISOString(),
    replies: [],
    ...overrides,
  }
}

describe('CommentItem', () => {
  const onDeleted = vi.fn()
  const onReplyCreated = vi.fn()

  beforeEach(() => {
    onDeleted.mockClear()
    onReplyCreated.mockClear()
    vi.mocked(createComment).mockReset()
    vi.mocked(deleteComment).mockReset()
  })

  it('renders author info and body', () => {
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    expect(screen.getByText('花子')).toBeInTheDocument()
    expect(screen.getByText('@hanako')).toBeInTheDocument()
    expect(screen.getByText('いいですね')).toBeInTheDocument()
  })

  it('links the author name and username to the profile page', () => {
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    expect(screen.getByText('花子')).toHaveAttribute('href', '/profile/hanako')
    expect(screen.getByText('@hanako')).toHaveAttribute('href', '/profile/hanako')
  })

  it('does not show a delete button when the current user is not the author', () => {
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    expect(screen.queryByRole('button', { name: '削除' })).not.toBeInTheDocument()
  })

  it('shows a delete button when the current user is the author', () => {
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={10}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    expect(screen.getByRole('button', { name: '削除' })).toBeInTheDocument()
  })

  it('deletes the comment when the confirm dialog is accepted', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(deleteComment).mockResolvedValue({ commentCount: 2 })
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={10}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: '削除' }))

    await waitFor(() => expect(deleteComment).toHaveBeenCalledWith(1))
    await waitFor(() => expect(onDeleted).toHaveBeenCalledWith(1, 2))
  })

  it('does not delete the comment when the confirm dialog is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={10}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: '削除' }))

    expect(deleteComment).not.toHaveBeenCalled()
    expect(onDeleted).not.toHaveBeenCalled()
  })

  it('opens and closes the reply form', async () => {
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: '返信' }))
    expect(screen.getByPlaceholderText('返信を入力...(1〜140文字)')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'キャンセル' }))
    expect(screen.queryByPlaceholderText('返信を入力...(1〜140文字)')).not.toBeInTheDocument()
  })

  it('submits a reply and calls onReplyCreated', async () => {
    const reply = makeComment({ id: 2, body: '返信です', parentCommentId: 1 })
    vi.mocked(createComment).mockResolvedValue(reply)
    render(
      <CommentItem
        comment={makeComment()}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: '返信' }))
    await userEvent.type(screen.getByPlaceholderText('返信を入力...(1〜140文字)'), '返信です')
    await userEvent.click(screen.getByRole('button', { name: '送信' }))

    await waitFor(() => expect(createComment).toHaveBeenCalledWith(1, '返信です', 1))
    await waitFor(() => expect(onReplyCreated).toHaveBeenCalledWith(reply, 1))
  })

  it('renders nested replies recursively', () => {
    const nested = makeComment({
      id: 1,
      replies: [makeComment({ id: 2, body: '子コメント', replies: [makeComment({ id: 3, body: '孫コメント' })] })],
    })
    render(
      <CommentItem
        comment={nested}
        postId={1}
        currentUserId={999}
        depth={0}
        onDeleted={onDeleted}
        onReplyCreated={onReplyCreated}
      />,
    )

    expect(screen.getByText('子コメント')).toBeInTheDocument()
    expect(screen.getByText('孫コメント')).toBeInTheDocument()
  })
})
