import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { likePost, unlikePost } from '../api/likes'
import { LikeButton } from './LikeButton'

vi.mock('../api/likes', () => ({
  likePost: vi.fn(),
  unlikePost: vi.fn(),
}))

describe('LikeButton', () => {
  const onChange = vi.fn()

  beforeEach(() => {
    onChange.mockClear()
    vi.mocked(likePost).mockReset()
    vi.mocked(unlikePost).mockReset()
  })

  it('shows the outlined heart and count when not liked', () => {
    render(<LikeButton postId={1} likeCount={3} isLikedByMe={false} onChange={onChange} />)

    expect(screen.getByText('♡')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('shows the filled heart when liked', () => {
    render(<LikeButton postId={1} likeCount={4} isLikedByMe={true} onChange={onChange} />)

    expect(screen.getByText('♥')).toBeInTheDocument()
  })

  it('calls likePost and onChange when clicked while not liked', async () => {
    vi.mocked(likePost).mockResolvedValue({ likeCount: 4, isLikedByMe: true })
    render(<LikeButton postId={1} likeCount={3} isLikedByMe={false} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button'))

    await waitFor(() => expect(likePost).toHaveBeenCalledWith(1))
    await waitFor(() => expect(onChange).toHaveBeenCalledWith({ likeCount: 4, isLikedByMe: true }))
  })

  it('calls unlikePost and onChange when clicked while liked', async () => {
    vi.mocked(unlikePost).mockResolvedValue({ likeCount: 2, isLikedByMe: false })
    render(<LikeButton postId={1} likeCount={3} isLikedByMe={true} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button'))

    await waitFor(() => expect(unlikePost).toHaveBeenCalledWith(1))
    await waitFor(() => expect(onChange).toHaveBeenCalledWith({ likeCount: 2, isLikedByMe: false }))
  })

  it('disables the button while a request is pending', async () => {
    let resolveRequest: (value: { likeCount: number; isLikedByMe: boolean }) => void = () => {}
    vi.mocked(likePost).mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )
    render(<LikeButton postId={1} likeCount={3} isLikedByMe={false} onChange={onChange} />)

    await userEvent.click(screen.getByRole('button'))

    expect(screen.getByRole('button')).toBeDisabled()
    resolveRequest({ likeCount: 4, isLikedByMe: true })
    await waitFor(() => expect(screen.getByRole('button')).not.toBeDisabled())
  })
})
