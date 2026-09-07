import { apiFetch } from './client'

export type UserSearchResult = {
  username: string
  displayName: string
  avatarUrl: string | null
}

export type UserSearchResponse = {
  results: UserSearchResult[]
  totalCount: number
}

export function searchUsers(keyword: string): Promise<UserSearchResponse> {
  return apiFetch(`/users/search?keyword=${encodeURIComponent(keyword)}`)
}
