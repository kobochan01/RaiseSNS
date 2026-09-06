import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getFollowers, getFollowing, type UserSummary } from '../api/follows'
import { avatarColor } from '../utils/avatar'

type Tab = 'following' | 'followers'

export function FollowListPage() {
  const { username } = useParams<{ username: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab: Tab = searchParams.get('tab') === 'followers' ? 'followers' : 'following'

  const [users, setUsers] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    if (!username) return
    let cancelled = false
    setLoading(true)
    setLoadError(null)
    const fetcher = activeTab === 'following' ? getFollowing : getFollowers
    fetcher(username)
      .then((result) => {
        if (!cancelled) setUsers(result)
      })
      .catch(() => {
        if (!cancelled) setLoadError('一覧の取得に失敗しました')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [username, activeTab])

  if (!username) {
    return null
  }

  return (
    <div className="profile-screen">
      <Link to={`/profile/${username}`} className="profile-back">
        ← 戻る
      </Link>
      <div className="tabs">
        <button
          type="button"
          className={`tab ${activeTab === 'following' ? 'is-active' : ''}`}
          onClick={() => setSearchParams({ tab: 'following' })}
        >
          フォロー中
        </button>
        <button
          type="button"
          className={`tab ${activeTab === 'followers' ? 'is-active' : ''}`}
          onClick={() => setSearchParams({ tab: 'followers' })}
        >
          フォロワー
        </button>
      </div>

      {loadError && <div className="form-error">{loadError}</div>}
      {!loading && users.length === 0 && !loadError && (
        <div className="empty-state">
          {activeTab === 'following' ? 'フォロー中のユーザーはいません。' : 'フォロワーはいません。'}
        </div>
      )}
      {users.map((row) => (
        <Link key={row.id} to={`/profile/${row.username}`} className="user-row">
          <div className="avatar avatar--md" style={{ backgroundColor: avatarColor(row.id) }}>
            {row.displayName.charAt(0)}
          </div>
          <div className="user-row__info">
            <div className="user-row__name">{row.displayName}</div>
            <div className="user-row__username">@{row.username}</div>
          </div>
        </Link>
      ))}
    </div>
  )
}
