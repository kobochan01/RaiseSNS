import { apiFetch } from './client'
import type { TimelineResponse } from './posts'

export type Profile = {
  id: number
  username: string
  displayName: string
  bio: string | null
  avatarUrl: string | null
  followerCount: number
  followingCount: number
  isFollowedByMe: boolean
}

export type UpdateProfileRequest = { displayName: string; bio: string | null }

export function getProfile(username: string): Promise<Profile> {
  return apiFetch(`/users/${username}`)
}

export function updateProfile(username: string, request: UpdateProfileRequest): Promise<Profile> {
  return apiFetch(`/users/${username}`, { method: 'PUT', body: JSON.stringify(request) })
}

export function getUserPosts(username: string, cursor?: number): Promise<TimelineResponse> {
  const params = new URLSearchParams()
  if (cursor !== undefined) {
    params.set('cursor', String(cursor))
  }
  const query = params.toString()
  return apiFetch(`/users/${username}/posts${query ? `?${query}` : ''}`)
}
