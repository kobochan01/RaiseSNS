import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { searchUsers } from './search'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('search api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
    vi.mocked(apiFetch).mockResolvedValue({ results: [], totalCount: 0 })
  })

  it('searchUsers encodes keyword in query string', async () => {
    await searchUsers('太郎 test&x')

    expect(apiFetch).toHaveBeenCalledWith(`/users/search?keyword=${encodeURIComponent('太郎 test&x')}`)
  })
})
