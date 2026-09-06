import { apiFetch } from './client'
import type { Author } from './posts'

export type Comment = {
  id: number
  author: Author
  body: string
  parentCommentId: number | null
  createdAt: string
  replies: Comment[]
}

export type CommentCountResponse = {
  commentCount: number
}

export function getComments(postId: number): Promise<Comment[]> {
  return apiFetch(`/posts/${postId}/comments`)
}

export function createComment(postId: number, body: string, parentCommentId?: number): Promise<Comment> {
  return apiFetch(`/posts/${postId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body, parentCommentId: parentCommentId ?? null }),
  })
}

export function deleteComment(commentId: number): Promise<CommentCountResponse> {
  return apiFetch(`/comments/${commentId}`, { method: 'DELETE' })
}
