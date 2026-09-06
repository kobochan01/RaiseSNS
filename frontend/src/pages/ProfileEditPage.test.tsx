import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getProfile, updateProfile, type Profile } from '../api/users'
import { AuthProvider, useAuth, type AuthUser } from '../context/AuthContext'
import { ProfileEditPage } from './ProfileEditPage'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/users', () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
}))

const currentUser: AuthUser = { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null }

function UserSetter({ user }: { user: AuthUser }) {
  const { setUser } = useAuth()
  useEffect(() => {
    setUser(user)
  }, [user, setUser])
  return null
}

function renderEditPage(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <UserSetter user={currentUser} />
        <Routes>
          <Route path="/profile/:username/edit" element={<ProfileEditPage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

function makeProfile(overrides: Partial<Profile> = {}): Profile {
  return {
    id: 1,
    username: 'taro',
    displayName: '太郎',
    bio: '自己紹介文',
    avatarUrl: null,
    followerCount: 0,
    followingCount: 0,
    isFollowedByMe: false,
    ...overrides,
  }
}

describe('ProfileEditPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
    vi.mocked(getProfile).mockReset()
    vi.mocked(updateProfile).mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('prefills the form with the current profile data', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())

    renderEditPage('/profile/taro/edit')

    expect(await screen.findByDisplayValue('太郎')).toBeInTheDocument()
    expect(screen.getByDisplayValue('自己紹介文')).toBeInTheDocument()
  })

  it('redirects to the profile page when the viewer is not the owner', async () => {
    renderEditPage('/profile/jiro/edit')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/profile/jiro', { replace: true }))
  })

  it('saves changes and navigates back to the profile page', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())
    vi.mocked(updateProfile).mockResolvedValue(makeProfile({ displayName: '新しい太郎' }))

    renderEditPage('/profile/taro/edit')
    const nameInput = await screen.findByDisplayValue('太郎')
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, '新しい太郎')
    await userEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => expect(updateProfile).toHaveBeenCalledWith('taro', { displayName: '新しい太郎', bio: '自己紹介文' }))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/profile/taro'))
  })

  it('disables save when the display name is blank', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())

    renderEditPage('/profile/taro/edit')
    const nameInput = await screen.findByDisplayValue('太郎')
    await userEvent.clear(nameInput)

    expect(screen.getByRole('button', { name: '保存' })).toBeDisabled()
  })

  it('disables save when the bio exceeds 160 characters', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())

    renderEditPage('/profile/taro/edit')
    const bioInput = await screen.findByDisplayValue('自己紹介文')
    await userEvent.clear(bioInput)
    await userEvent.type(bioInput, 'a'.repeat(161))

    expect(screen.getByRole('button', { name: '保存' })).toBeDisabled()
  })
})
