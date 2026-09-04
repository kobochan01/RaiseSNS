import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import { ApiError } from '../api/client'
import { createPost, getTimeline, type Post, type TimelineScope } from '../api/posts'
import { PostCard } from '../components/PostCard'
import { useAuth } from '../context/AuthContext'

const MAX_BODY_LENGTH = 280
const POLL_INTERVAL_MS = 30000

export function TimelinePage() {
  const { user, setUser } = useAuth()
  const navigate = useNavigate()

  const [activeTab, setActiveTab] = useState<TimelineScope>('all')
  const [posts, setPosts] = useState<Post[]>([])
  const [nextCursor, setNextCursor] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [pendingNewPosts, setPendingNewPosts] = useState<Post[]>([])

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newBody, setNewBody] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const sentinelRef = useRef<HTMLDivElement | null>(null)
  const postsRef = useRef<Post[]>([])
  postsRef.current = posts

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(null)
    setPendingNewPosts([])
    getTimeline(activeTab)
      .then((response) => {
        if (cancelled) return
        setPosts(response.posts)
        setNextCursor(response.nextCursor)
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError('タイムラインの取得に失敗しました')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [activeTab])

  useEffect(() => {
    const intervalId = setInterval(() => {
      if (document.hidden) return
      const latestId = postsRef.current[0]?.id ?? 0
      getTimeline(activeTab, undefined, undefined, latestId)
        .then((response) => {
          if (response.posts.length > 0) {
            setPendingNewPosts(response.posts)
          }
        })
        .catch(() => {
          // 新着チェックの失敗は画面表示に影響させない
        })
    }, POLL_INTERVAL_MS)
    return () => clearInterval(intervalId)
  }, [activeTab])

  function handleShowNewPosts() {
    setPosts((prev) => [...pendingNewPosts, ...prev])
    setPendingNewPosts([])
  }

  useEffect(() => {
    const sentinel = sentinelRef.current
    if (!sentinel || nextCursor === null || loading || loadingMore) {
      return
    }

    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        loadMore()
      }
    })
    observer.observe(sentinel)
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nextCursor, loading, loadingMore, activeTab])

  function loadMore() {
    if (nextCursor === null || loadingMore) return
    setLoadingMore(true)
    getTimeline(activeTab, nextCursor)
      .then((response) => {
        setPosts((prev) => [...prev, ...response.posts])
        setNextCursor(response.nextCursor)
      })
      .catch(() => {
        setLoadError('タイムラインの取得に失敗しました')
      })
      .finally(() => setLoadingMore(false))
  }

  async function handleCreateSubmit() {
    const body = newBody.trim()
    if (!body || body.length > MAX_BODY_LENGTH) return

    setCreating(true)
    setCreateError(null)
    try {
      const post = await createPost({ body })
      setPosts((prev) => [post, ...prev])
      setNewBody('')
      setShowCreateForm(false)
    } catch (err) {
      setCreateError(err instanceof ApiError ? err.message : '投稿に失敗しました')
    } finally {
      setCreating(false)
    }
  }

  function handlePostUpdated(updated: Post) {
    setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
  }

  function handlePostDeleted(postId: number) {
    setPosts((prev) => prev.filter((p) => p.id !== postId))
  }

  async function handleLogout() {
    try {
      await logout()
    } catch (err) {
      console.error(err)
    } finally {
      setUser(null)
      navigate('/login')
    }
  }

  return (
    <div className="timeline-screen">
      <header className="timeline-header">
        <span className="timeline-header__title">RaiseSNS</span>
        <button type="button" className="btn btn--outline btn--sm" onClick={handleLogout}>
          ログアウト
        </button>
      </header>

      <div className="tabs">
        <button
          type="button"
          className={`tab ${activeTab === 'all' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('all')}
        >
          全体
        </button>
        <button
          type="button"
          className={`tab ${activeTab === 'following' ? 'is-active' : ''}`}
          onClick={() => setActiveTab('following')}
        >
          フォロー中
        </button>
      </div>

      <div className="timeline-toolbar">
        <button type="button" className="btn btn--primary" onClick={() => setShowCreateForm((v) => !v)}>
          ＋ 投稿
        </button>
      </div>

      {showCreateForm && (
        <div className="post-create">
          <textarea
            className="post-create__textarea"
            placeholder="今なにしてる?"
            value={newBody}
            onChange={(e) => setNewBody(e.target.value)}
            maxLength={500}
          />
          <div className="field-meta">
            <span></span>
            <span className={`char-counter ${newBody.length > MAX_BODY_LENGTH ? 'is-over' : ''}`}>
              {newBody.length} / {MAX_BODY_LENGTH}
            </span>
          </div>
          {createError && <div className="form-error">{createError}</div>}
          <div className="post-create__footer">
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleCreateSubmit}
              disabled={creating || newBody.trim().length === 0 || newBody.length > MAX_BODY_LENGTH}
            >
              投稿する
            </button>
          </div>
        </div>
      )}

      {loadError && <div className="form-error">{loadError}</div>}

      {pendingNewPosts.length > 0 && (
        <button type="button" className="new-posts-banner" onClick={handleShowNewPosts}>
          新しい投稿があります
        </button>
      )}

      {!loading && posts.length === 0 && (
        <div className="empty-state">
          まだ投稿がありません。「フォロー中」タブの場合は、ユーザーをフォローすると表示されます。
        </div>
      )}

      {user &&
        posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            currentUserId={user.id}
            onUpdated={handlePostUpdated}
            onDeleted={handlePostDeleted}
          />
        ))}

      <div ref={sentinelRef} className="timeline-load-more" />
    </div>
  )
}
