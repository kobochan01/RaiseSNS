import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { login, logout, refresh, register } from './auth'

vi.mock('./client', () => ({
  apiFetch: vi.fn(),
}))

describe('auth api', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockReset()
  })

  it('register calls apiFetch with POST and request body', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ id: 1, username: 'taro', displayName: '太郎' })
    const request = { username: 'taro', email: 'taro@example.com', password: 'password123', displayName: '太郎' }

    await register(request)

    expect(apiFetch).toHaveBeenCalledWith('/auth/register', { method: 'POST', body: JSON.stringify(request) })
  })

  it('login calls apiFetch with POST and request body', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ id: 1, username: 'taro', displayName: '太郎', avatarUrl: null })
    const request = { email: 'taro@example.com', password: 'password123' }

    await login(request)

    expect(apiFetch).toHaveBeenCalledWith('/auth/login', { method: 'POST', body: JSON.stringify(request) })
  })

  it('logout calls apiFetch with POST and no body', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)

    await logout()

    expect(apiFetch).toHaveBeenCalledWith('/auth/logout', { method: 'POST' })
  })

  it('refresh calls apiFetch with POST and no body', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ id: 1, username: 'taro', displayName: '太郎', avatarUrl: null })

    await refresh()

    expect(apiFetch).toHaveBeenCalledWith('/auth/refresh', { method: 'POST' })
  })
})
