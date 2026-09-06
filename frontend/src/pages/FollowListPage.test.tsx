import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getFollowers, getFollowing, type UserSummary } from '../api/follows'
import { FollowListPage } from './FollowListPage'

vi.mock('../api/follows', () => ({
  getFollowing: vi.fn(),
  getFollowers: vi.fn(),
}))

function renderFollowListPage(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/profile/:username/connections" element={<FollowListPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

function makeUser(overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id: 2,
    username: 'jiro',
    displayName: '次郎',
    avatarUrl: null,
    isFollowedByMe: false,
    ...overrides,
  }
}

describe('FollowListPage', () => {
  beforeEach(() => {
    vi.mocked(getFollowing).mockReset()
    vi.mocked(getFollowers).mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the following tab by default and lists users', async () => {
    vi.mocked(getFollowing).mockResolvedValue([makeUser()])

    renderFollowListPage('/profile/taro/connections')

    expect(await screen.findByText('次郎')).toBeInTheDocument()
    expect(screen.getByText('@jiro')).toBeInTheDocument()
    expect(getFollowing).toHaveBeenCalledWith('taro')
  })

  it('shows the followers tab when the query param says so', async () => {
    vi.mocked(getFollowers).mockResolvedValue([makeUser({ username: 'saburo', displayName: '三郎' })])

    renderFollowListPage('/profile/taro/connections?tab=followers')

    expect(await screen.findByText('三郎')).toBeInTheDocument()
    expect(getFollowers).toHaveBeenCalledWith('taro')
  })

  it('switches tabs when clicked', async () => {
    vi.mocked(getFollowing).mockResolvedValue([])
    vi.mocked(getFollowers).mockResolvedValue([makeUser({ username: 'shiro', displayName: '四郎' })])

    renderFollowListPage('/profile/taro/connections')
    await waitFor(() => expect(getFollowing).toHaveBeenCalled())

    await userEvent.click(screen.getByRole('button', { name: 'フォロワー' }))

    expect(await screen.findByText('四郎')).toBeInTheDocument()
  })

  it('links each row to the user profile', async () => {
    vi.mocked(getFollowing).mockResolvedValue([makeUser()])

    renderFollowListPage('/profile/taro/connections')

    expect(await screen.findByText('次郎')).toBeInTheDocument()
    expect(screen.getByText('次郎').closest('a')).toHaveAttribute('href', '/profile/jiro')
  })

  it('shows the empty state when there are no users', async () => {
    vi.mocked(getFollowing).mockResolvedValue([])

    renderFollowListPage('/profile/taro/connections')

    expect(await screen.findByText('フォロー中のユーザーはいません。')).toBeInTheDocument()
  })
})
