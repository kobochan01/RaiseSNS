import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { followUser, unfollowUser } from '../api/follows'
import type { Post } from '../api/posts'
import { getProfile, getUserPosts, type Profile } from '../api/users'
import { AuthProvider, useAuth, type AuthUser } from '../context/AuthContext'
import { ProfilePage } from './ProfilePage'

vi.mock('../api/users', () => ({
  getProfile: vi.fn(),
  getUserPosts: vi.fn(),
}))

vi.mock('../api/follows', () => ({
  followUser: vi.fn(),
  unfollowUser: vi.fn(),
}))

const currentUser: AuthUser = { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null }

function UserSetter({ user }: { user: AuthUser }) {
  const { setUser } = useAuth()
  useEffect(() => {
    setUser(user)
  }, [user, setUser])
  return null
}

function renderProfilePage(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <UserSetter user={currentUser} />
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

function makeProfile(overrides: Partial<Profile> = {}): Profile {
  return {
    id: 2,
    username: 'jiro',
    displayName: '次郎',
    bio: 'よろしくお願いします',
    avatarUrl: null,
    followerCount: 3,
    followingCount: 5,
    isFollowedByMe: false,
    ...overrides,
  }
}

function makePost(overrides: Partial<Post> = {}): Post {
  return {
    id: 1,
    author: { id: 2, username: 'jiro', displayName: '次郎', avatarUrl: null },
    body: 'こんにちは',
    imageUrl: null,
    likeCount: 0,
    commentCount: 0,
    isLikedByMe: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  }
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.mocked(getProfile).mockReset()
    vi.mocked(getUserPosts).mockReset()
    vi.mocked(followUser).mockReset()
    vi.mocked(unfollowUser).mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders profile header and posts', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [makePost()], nextCursor: null })

    renderProfilePage('/profile/jiro')

    expect(await screen.findByText('次郎', { selector: '.profile-name' })).toBeInTheDocument()
    expect(screen.getAllByText('@jiro').length).toBeGreaterThan(0)
    expect(screen.getByText('よろしくお願いします')).toBeInTheDocument()
    expect(screen.getByText('こんにちは')).toBeInTheDocument()
  })

  it('shows an edit link instead of a follow button on my own profile', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile({ username: 'taro', id: 1 }))
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })

    renderProfilePage('/profile/taro')

    expect(await screen.findByRole('link', { name: '編集' })).toHaveAttribute('href', '/profile/taro/edit')
    expect(screen.queryByRole('button', { name: 'フォロー' })).not.toBeInTheDocument()
  })

  it('shows a follow button on another user profile and toggles on click', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })
    vi.mocked(followUser).mockResolvedValue({ followerCount: 4, isFollowedByMe: true })

    renderProfilePage('/profile/jiro')

    const followButton = await screen.findByRole('button', { name: 'フォロー' })
    await userEvent.click(followButton)

    await waitFor(() => expect(followUser).toHaveBeenCalledWith('jiro'))
    expect(await screen.findByRole('button', { name: 'フォロー中' })).toBeInTheDocument()
  })

  it('unfollows when already following', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile({ isFollowedByMe: true }))
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })
    vi.mocked(unfollowUser).mockResolvedValue({ followerCount: 2, isFollowedByMe: false })

    renderProfilePage('/profile/jiro')

    const followButton = await screen.findByRole('button', { name: 'フォロー中' })
    await userEvent.click(followButton)

    await waitFor(() => expect(unfollowUser).toHaveBeenCalledWith('jiro'))
    expect(await screen.findByRole('button', { name: 'フォロー' })).toBeInTheDocument()
  })

  it('shows the follower/following counts linking to the connections page', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })

    renderProfilePage('/profile/jiro')

    await screen.findByText('次郎')
    expect(screen.getByText('5').closest('a')).toHaveAttribute('href', '/profile/jiro/connections?tab=following')
    expect(screen.getByText('3').closest('a')).toHaveAttribute('href', '/profile/jiro/connections?tab=followers')
  })

  it('shows the empty state when the user has no posts', async () => {
    vi.mocked(getProfile).mockResolvedValue(makeProfile())
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })

    renderProfilePage('/profile/jiro')

    expect(await screen.findByText('まだ投稿がありません。')).toBeInTheDocument()
  })

  it('shows an error message when the user is not found', async () => {
    vi.mocked(getProfile).mockRejectedValue(new Error('not found'))
    vi.mocked(getUserPosts).mockResolvedValue({ posts: [], nextCursor: null })

    renderProfilePage('/profile/nobody')

    expect(await screen.findByText('プロフィールの取得に失敗しました')).toBeInTheDocument()
  })
})
