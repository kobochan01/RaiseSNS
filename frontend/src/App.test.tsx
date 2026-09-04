import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { refresh } from './api/auth'
import { ApiError } from './api/client'
import { getTimeline } from './api/posts'
import { AuthProvider } from './context/AuthContext'

vi.mock('./api/auth', () => ({
  refresh: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('./api/posts', () => ({
  getTimeline: vi.fn(),
}))

function renderApp() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('App', () => {
  it('shows the timeline screen when the silent refresh succeeds', async () => {
    vi.mocked(refresh).mockResolvedValue({ id: 1, username: 'taro', displayName: '太郎', avatarUrl: null })
    vi.mocked(getTimeline).mockResolvedValue({ posts: [], nextCursor: null })

    renderApp()

    expect(await screen.findByRole('button', { name: '全体' })).toBeInTheDocument()
  })

  it('shows the login screen when the silent refresh fails', async () => {
    vi.mocked(refresh).mockRejectedValue(new ApiError(401, 'リフレッシュトークンが無効です', null))

    renderApp()

    expect(await screen.findByText('おかえりなさい')).toBeInTheDocument()
  })
})
