import { apiFetch } from './client'

export type FollowResponse = {
  followerCount: number
  isFollowedByMe: boolean
}

export type UserSummary = {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  isFollowedByMe: boolean
}

export function followUser(username: string): Promise<FollowResponse> {
  return apiFetch(`/users/${username}/follow`, { method: 'POST' })
}

export function unfollowUser(username: string): Promise<FollowResponse> {
  return apiFetch(`/users/${username}/follow`, { method: 'DELETE' })
}

export function getFollowing(username: string): Promise<UserSummary[]> {
  return apiFetch<{ users: UserSummary[] }>(`/users/${username}/following`).then((res) => res.users)
}

export function getFollowers(username: string): Promise<UserSummary[]> {
  return apiFetch<{ users: UserSummary[] }>(`/users/${username}/followers`).then((res) => res.users)
}
