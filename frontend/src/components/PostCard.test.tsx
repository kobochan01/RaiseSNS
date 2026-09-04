import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { deletePost, updatePost, type Post } from '../api/posts'
import { PostCard } from './PostCard'

vi.mock('../api/posts', () => ({
  updatePost: vi.fn(),
  deletePost: vi.fn(),
}))

const basePost: Post = {
  id: 1,
  author: { id: 10, username: 'taro', displayName: '太郎', avatarUrl: null },
  body: '元の本文',
  imageUrl: null,
  likeCount: 0,
  commentCount: 0,
  isLikedByMe: false,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

describe('PostCard', () => {
  const onUpdated = vi.fn()
  const onDeleted = vi.fn()

  beforeEach(() => {
    onUpdated.mockClear()
    onDeleted.mockClear()
    vi.mocked(updatePost).mockReset()
    vi.mocked(deletePost).mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders author info and body', () => {
    render(<PostCard post={basePost} currentUserId={999} onUpdated={onUpdated} onDeleted={onDeleted} />)

    expect(screen.getByText('太郎')).toBeInTheDocument()
    expect(screen.getByText('@taro')).toBeInTheDocument()
    expect(screen.getByText('元の本文')).toBeInTheDocument()
  })

  it('does not show edit/delete links when the current user is not the author', () => {
    render(<PostCard post={basePost} currentUserId={999} onUpdated={onUpdated} onDeleted={onDeleted} />)

    expect(screen.queryByRole('button', { name: '編集' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '削除' })).not.toBeInTheDocument()
  })

  it('shows edit/delete links when the current user is the author', () => {
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)

    expect(screen.getByRole('button', { name: '編集' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '削除' })).toBeInTheDocument()
  })

  it('switches to a textarea prefilled with the body when editing starts', async () => {
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)

    await userEvent.click(screen.getByRole('button', { name: '編集' }))

    expect(screen.getByDisplayValue('元の本文')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '保存' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'キャンセル' })).toBeInTheDocument()
  })

  it('disables save when the body exceeds 280 characters', async () => {
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)
    await userEvent.click(screen.getByRole('button', { name: '編集' }))

    const textarea = screen.getByDisplayValue('元の本文')
    await userEvent.clear(textarea)
    await userEvent.type(textarea, 'a'.repeat(281))

    expect(screen.getByRole('button', { name: '保存' })).toBeDisabled()
  })

  it('saves the edit and exits edit mode on success', async () => {
    const updated = { ...basePost, body: '更新後の本文' }
    vi.mocked(updatePost).mockResolvedValue(updated)
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)
    await userEvent.click(screen.getByRole('button', { name: '編集' }))

    const textarea = screen.getByDisplayValue('元の本文')
    await userEvent.clear(textarea)
    await userEvent.type(textarea, '更新後の本文')
    await userEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => expect(updatePost).toHaveBeenCalledWith(1, { body: '更新後の本文' }))
    await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(updated))
    expect(screen.queryByRole('button', { name: '保存' })).not.toBeInTheDocument()
  })

  it('cancels editing without calling the API', async () => {
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)
    await userEvent.click(screen.getByRole('button', { name: '編集' }))

    await userEvent.click(screen.getByRole('button', { name: 'キャンセル' }))

    expect(updatePost).not.toHaveBeenCalled()
    expect(screen.getByText('元の本文')).toBeInTheDocument()
  })

  it('deletes the post when the confirm dialog is accepted', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(deletePost).mockResolvedValue(undefined)
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)

    await userEvent.click(screen.getByRole('button', { name: '削除' }))

    await waitFor(() => expect(deletePost).toHaveBeenCalledWith(1))
    await waitFor(() => expect(onDeleted).toHaveBeenCalledWith(1))
  })

  it('does not delete the post when the confirm dialog is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    render(<PostCard post={basePost} currentUserId={10} onUpdated={onUpdated} onDeleted={onDeleted} />)

    await userEvent.click(screen.getByRole('button', { name: '削除' }))

    expect(deletePost).not.toHaveBeenCalled()
    expect(onDeleted).not.toHaveBeenCalled()
  })
})
