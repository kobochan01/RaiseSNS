import { apiFetch } from './client'

export type LikeResponse = {
  likeCount: number
  isLikedByMe: boolean
}

export function likePost(postId: number): Promise<LikeResponse> {
  return apiFetch(`/posts/${postId}/likes`, { method: 'POST' })
}

export function unlikePost(postId: number): Promise<LikeResponse> {
  return apiFetch(`/posts/${postId}/likes`, { method: 'DELETE' })
}
