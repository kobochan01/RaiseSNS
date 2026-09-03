import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { logout } from '../api/auth'
import { AuthProvider, useAuth, type AuthUser } from '../context/AuthContext'
import { HomePage } from './HomePage'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/auth', () => ({
  logout: vi.fn(),
}))

const currentUser: AuthUser = { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null }

function UserSetter({ user }: { user: AuthUser }) {
  const { setUser } = useAuth()
  useEffect(() => {
    setUser(user)
  }, [user, setUser])
  return null
}

function renderHomePage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <UserSetter user={currentUser} />
        <HomePage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('HomePage', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
    vi.mocked(logout).mockReset()
  })

  it('shows the login success message with the display name', async () => {
    renderHomePage()

    expect(await screen.findByText('ログイン成功')).toBeInTheDocument()
    expect(screen.getByText('ようこそ、太郎さん')).toBeInTheDocument()
  })

  it('logs out and navigates to login when the logout button is clicked', async () => {
    vi.mocked(logout).mockResolvedValue(undefined)
    renderHomePage()

    await userEvent.click(await screen.findByRole('button', { name: 'ログアウト' }))

    await waitFor(() => expect(logout).toHaveBeenCalled())
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login'))
  })
})
