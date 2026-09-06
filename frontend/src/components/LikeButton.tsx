import { useState } from 'react'
import { likePost, unlikePost, type LikeResponse } from '../api/likes'

type Props = {
  postId: number
  likeCount: number
  isLikedByMe: boolean
  onChange: (next: LikeResponse) => void
}

export function LikeButton({ postId, likeCount, isLikedByMe, onChange }: Props) {
  const [pending, setPending] = useState(false)

  async function handleClick(e: React.MouseEvent) {
    e.stopPropagation()
    if (pending) return
    setPending(true)
    try {
      const response = isLikedByMe ? await unlikePost(postId) : await likePost(postId)
      onChange(response)
    } catch {
      // いいね操作の失敗時は現状の表示を維持する
    } finally {
      setPending(false)
    }
  }

  return (
    <button
      type="button"
      className={`like-button ${isLikedByMe ? 'is-liked' : ''}`}
      onClick={handleClick}
      disabled={pending}
    >
      <span className="like-button__icon">{isLikedByMe ? '♥' : '♡'}</span>
      <span className="like-button__count">{likeCount}</span>
    </button>
  )
}
