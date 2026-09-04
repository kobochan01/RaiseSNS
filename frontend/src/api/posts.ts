import { apiFetch } from './client'

export type Author = {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
}

export type Post = {
  id: number
  author: Author
  body: string
  imageUrl: string | null
  likeCount: number
  commentCount: number
  isLikedByMe: boolean
  createdAt: string
  updatedAt: string
}

export type TimelineScope = 'all' | 'following'

export type TimelineResponse = {
  posts: Post[]
  nextCursor: number | null
}

export type CreatePostRequest = { body: string }
export type UpdatePostRequest = { body: string }

export function getTimeline(
  scope: TimelineScope,
  cursor?: number,
  limit = 20,
  sinceId?: number,
): Promise<TimelineResponse> {
  const params = new URLSearchParams({ scope, limit: String(limit) })
  if (sinceId !== undefined) {
    params.set('sinceId', String(sinceId))
  } else if (cursor !== undefined) {
    params.set('cursor', String(cursor))
  }
  return apiFetch(`/posts?${params.toString()}`)
}

export function createPost(request: CreatePostRequest): Promise<Post> {
  return apiFetch('/posts', { method: 'POST', body: JSON.stringify(request) })
}

export function updatePost(id: number, request: UpdatePostRequest): Promise<Post> {
  return apiFetch(`/posts/${id}`, { method: 'PUT', body: JSON.stringify(request) })
}

export function deletePost(id: number): Promise<void> {
  return apiFetch(`/posts/${id}`, { method: 'DELETE' })
}
