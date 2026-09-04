import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { logout } from '../api/auth'
import { createPost, getTimeline, type Post, type TimelineResponse } from '../api/posts'
import { AuthProvider, useAuth, type AuthUser } from '../context/AuthContext'
import { TimelinePage } from './TimelinePage'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/auth', () => ({
  logout: vi.fn(),
}))

vi.mock('../api/posts', () => ({
  getTimeline: vi.fn(),
  createPost: vi.fn(),
}))

class MockIntersectionObserver implements IntersectionObserver {
  readonly root = null
  readonly rootMargin = ''
  readonly scrollMargin = ''
  readonly thresholds: ReadonlyArray<number> = []
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
  takeRecords = () => []
}

vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)

const currentUser: AuthUser = { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null }

function UserSetter({ user }: { user: AuthUser }) {
  const { setUser } = useAuth()
  useEffect(() => {
    setUser(user)
  }, [user, setUser])
  return null
}

function renderTimelinePage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <UserSetter user={currentUser} />
        <TimelinePage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

function makePost(overrides: Partial<Post> = {}): Post {
  return {
    id: 1,
    author: { id: 1, username: 'taro', displayName: '太郎', avatarUrl: null },
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

const emptyResponse: TimelineResponse = { posts: [], nextCursor: null }

describe('TimelinePage', () => {
  beforeEach(() => {
    mockNavigate.mockClear()
    vi.mocked(logout).mockReset()
    vi.mocked(getTimeline).mockReset()
    vi.mocked(createPost).mockReset()
    vi.mocked(getTimeline).mockResolvedValue(emptyResponse)
  })

  afterEach(() => {
    vi.useRealTimers()
    Object.defineProperty(document, 'hidden', { value: false, configurable: true })
  })

  it('loads the all-timeline on mount with the all tab active', async () => {
    renderTimelinePage()

    await waitFor(() => expect(getTimeline).toHaveBeenCalledWith('all'))
    expect(await screen.findByRole('button', { name: '全体' })).toHaveClass('is-active')
  })

  it('shows the empty state when there are no posts', async () => {
    renderTimelinePage()

    expect(
      await screen.findByText('まだ投稿がありません。「フォロー中」タブの場合は、ユーザーをフォローすると表示されます。'),
    ).toBeInTheDocument()
  })

  it('switches to the following tab and requests the following scope', async () => {
    renderTimelinePage()
    await screen.findByRole('button', { name: '全体' })

    await userEvent.click(screen.getByRole('button', { name: 'フォロー中' }))

    await waitFor(() => expect(getTimeline).toHaveBeenCalledWith('following'))
    expect(screen.getByRole('button', { name: 'フォロー中' })).toHaveClass('is-active')
  })

  it('renders posts returned from the timeline', async () => {
    vi.mocked(getTimeline).mockResolvedValue({ posts: [makePost({ body: '最初の投稿' })], nextCursor: null })

    renderTimelinePage()

    expect(await screen.findByText('最初の投稿')).toBeInTheDocument()
  })

  it('toggles the create form when the post button is clicked', async () => {
    renderTimelinePage()
    await screen.findByRole('button', { name: '全体' })

    expect(screen.queryByPlaceholderText('今なにしてる?')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '＋ 投稿' }))
    expect(screen.getByPlaceholderText('今なにしてる?')).toBeInTheDocument()
  })

  it('disables the submit button when the body is empty', async () => {
    renderTimelinePage()
    await screen.findByRole('button', { name: '全体' })
    await userEvent.click(screen.getByRole('button', { name: '＋ 投稿' }))

    expect(screen.getByRole('button', { name: '投稿する' })).toBeDisabled()
  })

  it('creates a post and prepends it to the list on submit', async () => {
    const created = makePost({ id: 999, body: '新しい投稿' })
    vi.mocked(createPost).mockResolvedValue(created)
    renderTimelinePage()
    await screen.findByRole('button', { name: '全体' })
    await userEvent.click(screen.getByRole('button', { name: '＋ 投稿' }))

    await userEvent.type(screen.getByPlaceholderText('今なにしてる?'), '新しい投稿')
    await userEvent.click(screen.getByRole('button', { name: '投稿する' }))

    await waitFor(() => expect(createPost).toHaveBeenCalledWith({ body: '新しい投稿' }))
    expect(await screen.findByText('新しい投稿')).toBeInTheDocument()
    expect(screen.queryByPlaceholderText('今なにしてる?')).not.toBeInTheDocument()
  })

  it('polls for new posts every 30 seconds and shows a notification banner', async () => {
    vi.useFakeTimers()
    const initialPost = makePost({ id: 5, body: '最初の投稿' })
    const newPost = makePost({ id: 6, body: '新着投稿' })
    vi.mocked(getTimeline).mockImplementation((_scope, _cursor, _limit, sinceId) =>
      Promise.resolve(
        sinceId !== undefined
          ? { posts: [newPost], nextCursor: null }
          : { posts: [initialPost], nextCursor: null },
      ),
    )

    renderTimelinePage()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(screen.getByText('最初の投稿')).toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30000)
    })

    expect(getTimeline).toHaveBeenCalledWith('all', undefined, undefined, 5)
    expect(screen.getByRole('button', { name: '新しい投稿があります' })).toBeInTheDocument()
  })

  it('prepends pending new posts to the list when the notification banner is clicked', async () => {
    vi.useFakeTimers()
    const initialPost = makePost({ id: 5, body: '最初の投稿' })
    const newPost = makePost({ id: 6, body: '新着投稿' })
    vi.mocked(getTimeline).mockImplementation((_scope, _cursor, _limit, sinceId) =>
      Promise.resolve(
        sinceId !== undefined
          ? { posts: [newPost], nextCursor: null }
          : { posts: [initialPost], nextCursor: null },
      ),
    )

    renderTimelinePage()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30000)
    })

    fireEvent.click(screen.getByRole('button', { name: '新しい投稿があります' }))

    expect(screen.getByText('新着投稿')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '新しい投稿があります' })).not.toBeInTheDocument()
  })

  it('does not check for new posts while the page is hidden', async () => {
    vi.useFakeTimers()
    Object.defineProperty(document, 'hidden', { value: true, configurable: true })
    vi.mocked(getTimeline).mockResolvedValue(emptyResponse)

    renderTimelinePage()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    vi.mocked(getTimeline).mockClear()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(30000)
    })

    expect(getTimeline).not.toHaveBeenCalled()
  })

  it('logs out and navigates to login when the logout button is clicked', async () => {
    vi.mocked(logout).mockResolvedValue(undefined)
    renderTimelinePage()
    await screen.findByRole('button', { name: '全体' })

    await userEvent.click(screen.getByRole('button', { name: 'ログアウト' }))

    await waitFor(() => expect(logout).toHaveBeenCalled())
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/login'))
  })
})
