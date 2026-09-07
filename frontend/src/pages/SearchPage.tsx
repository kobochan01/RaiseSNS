import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { searchUsers, type UserSearchResult } from '../api/search'
import { avatarColor } from '../utils/avatar'

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const keyword = (searchParams.get('q') ?? '').trim()

  const [results, setResults] = useState<UserSearchResult[]>([])
  const [totalCount, setTotalCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!keyword) {
      setResults([])
      setTotalCount(0)
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    searchUsers(keyword)
      .then((response) => {
        if (cancelled) return
        setResults(response.results)
        setTotalCount(response.totalCount)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : '検索に失敗しました')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [keyword])

  return (
    <div className="profile-screen">
      <Link to="/" className="profile-back">
        ← 戻る
      </Link>

      {!keyword && <div className="empty-state">ユーザー名(@username)を入力して検索してください。</div>}

      {keyword && (
        <>
          <div className="search-result-count">「{keyword}」の検索結果({totalCount}件)</div>
          {error && <div className="form-error">{error}</div>}
          {!loading && !error && results.length === 0 && (
            <div className="empty-state">該当するユーザーが見つかりませんでした。</div>
          )}
          {results.map((result) => (
            <Link key={result.username} to={`/profile/${result.username}`} className="user-row">
              <div className="avatar avatar--md" style={{ backgroundColor: avatarColor(result.username) }}>
                {result.displayName.charAt(0)}
              </div>
              <div className="user-row__info">
                <div className="user-row__name">{result.displayName}</div>
                <div className="user-row__username">@{result.username}</div>
              </div>
            </Link>
          ))}
        </>
      )}
    </div>
  )
}
