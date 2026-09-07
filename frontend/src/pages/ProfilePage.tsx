import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { followUser, unfollowUser } from '../api/follows'
import type { Post } from '../api/posts'
import { getProfile, getUserPosts, type Profile } from '../api/users'
import { Avatar } from '../components/Avatar'
import { PostCard } from '../components/PostCard'
import { PostDetailModal } from '../components/PostDetailModal'
import { useAuth } from '../context/AuthContext'

export function ProfilePage() {
  const { username } = useParams<{ username: string }>()
  const { user } = useAuth()

  const [profile, setProfile] = useState<Profile | null>(null)
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [followBusy, setFollowBusy] = useState(false)
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)

  useEffect(() => {
    if (!username) return
    let cancelled = false
    setLoading(true)
    setLoadError(null)
    Promise.all([getProfile(username), getUserPosts(username)])
      .then(([profileResult, postsResult]) => {
        if (cancelled) return
        setProfile(profileResult)
        setPosts(postsResult.posts)
      })
      .catch(() => {
        if (!cancelled) setLoadError('プロフィールの取得に失敗しました')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [username])

  async function handleFollowToggle() {
    if (!profile || followBusy) return
    setFollowBusy(true)
    try {
      const result = profile.isFollowedByMe ? await unfollowUser(profile.username) : await followUser(profile.username)
      setProfile({ ...profile, isFollowedByMe: result.isFollowedByMe, followerCount: result.followerCount })
    } finally {
      setFollowBusy(false)
    }
  }

  function handlePostUpdated(updated: Post) {
    setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
  }

  function handlePostDeleted(postId: number) {
    setPosts((prev) => prev.filter((p) => p.id !== postId))
  }

  if (loading) {
    return <div className="empty-state">読み込み中...</div>
  }

  if (loadError || !profile) {
    return (
      <>
        <Link to="/" className="profile-back">
          ← 戻る
        </Link>
        <div className="empty-state">{loadError ?? 'ユーザーが見つかりませんでした。'}</div>
      </>
    )
  }

  const isSelf = user?.username === profile.username

  return (
    <div className="profile-screen">
      <Link to="/timeline" className="profile-back">
        ← 戻る
      </Link>
      <div className="profile-header">
        <div className="profile-top">
          <Avatar userId={profile.id} displayName={profile.displayName} avatarUrl={profile.avatarUrl} size="lg" />
          {isSelf ? (
            <Link to={`/profile/${profile.username}/edit`} className="btn btn--outline btn--sm">
              編集
            </Link>
          ) : (
            <button
              type="button"
              className={`btn ${profile.isFollowedByMe ? 'btn--outline' : 'btn--primary'} btn--sm`}
              onClick={handleFollowToggle}
              disabled={followBusy}
            >
              {profile.isFollowedByMe ? 'フォロー中' : 'フォロー'}
            </button>
          )}
        </div>
        <div className="profile-name">{profile.displayName}</div>
        <div className="profile-username">@{profile.username}</div>
        {profile.bio && <div className="profile-bio">{profile.bio}</div>}
        <div className="profile-stats">
          <Link to={`/profile/${profile.username}/connections?tab=following`}>
            フォロー中 <strong>{profile.followingCount}</strong>
          </Link>
          <Link to={`/profile/${profile.username}/connections?tab=followers`}>
            フォロワー <strong>{profile.followerCount}</strong>
          </Link>
        </div>
      </div>
      <div className="profile-posts-title">投稿</div>
      {posts.length === 0 && <div className="empty-state">まだ投稿がありません。</div>}
      {user &&
        posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            currentUserId={user.id}
            onUpdated={handlePostUpdated}
            onDeleted={handlePostDeleted}
            onOpenDetail={setSelectedPostId}
          />
        ))}

      {user &&
        selectedPostId !== null &&
        (() => {
          const selectedPost = posts.find((p) => p.id === selectedPostId)
          if (!selectedPost) return null
          return (
            <PostDetailModal
              post={selectedPost}
              currentUserId={user.id}
              onClose={() => setSelectedPostId(null)}
              onUpdated={handlePostUpdated}
            />
          )
        })()}
    </div>
  )
}
