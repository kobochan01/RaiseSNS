import { useEffect, useState, type ChangeEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { uploadImage } from '../api/images'
import { getProfile, updateProfile } from '../api/users'
import { Avatar } from '../components/Avatar'
import { useAuth } from '../context/AuthContext'

const DISPLAY_NAME_MAX = 50
const BIO_MAX = 160
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

export function ProfileEditPage() {
  const { username } = useParams<{ username: string }>()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [bio, setBio] = useState('')
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploadingAvatar, setUploadingAvatar] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isSelf = user?.username === username

  useEffect(() => {
    if (!username || !isSelf) return
    let cancelled = false
    getProfile(username).then((profile) => {
      if (cancelled) return
      setDisplayName(profile.displayName)
      setBio(profile.bio ?? '')
      setAvatarUrl(profile.avatarUrl)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [username, isSelf])

  useEffect(() => {
    if (username && !isSelf) {
      navigate(`/profile/${username}`, { replace: true })
    }
  }, [username, isSelf, navigate])

  if (!username || !isSelf) {
    return null
  }

  if (loading) {
    return <div className="empty-state">読み込み中...</div>
  }

  const trimmedDisplayName = displayName.trim()
  const canSave =
    trimmedDisplayName.length > 0 && trimmedDisplayName.length <= DISPLAY_NAME_MAX && bio.length <= BIO_MAX

  async function handleSave() {
    if (!username || !canSave) return
    setSaving(true)
    setError(null)
    try {
      await updateProfile(username, { displayName: trimmedDisplayName, bio, avatarUrl })
      navigate(`/profile/${username}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '更新に失敗しました')
    } finally {
      setSaving(false)
    }
  }

  async function handleAvatarSelect(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      alert('画像サイズは5MB以下にしてください。')
      e.target.value = ''
      return
    }
    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      alert('jpg・jpeg・png・gif・webp形式のみアップロードできます。')
      e.target.value = ''
      return
    }

    setUploadingAvatar(true)
    setError(null)
    try {
      const result = await uploadImage(file, 'avatar')
      setAvatarUrl(result.imageUrl)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '画像のアップロードに失敗しました')
    } finally {
      setUploadingAvatar(false)
      e.target.value = ''
    }
  }

  return (
    <div className="profile-screen">
      <Link to={`/profile/${username}`} className="profile-back">
        ← 戻る
      </Link>
      <div className="profile-header">
        <h2 className="auth-title">プロフィール編集</h2>
        <div className="form-field">
          <label>アイコン画像</label>
          <Avatar userId={user?.id ?? 0} displayName={displayName || '?'} avatarUrl={avatarUrl} size="lg" />
          <label className="file-label">
            🖼 画像を選択
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleAvatarSelect}
              disabled={uploadingAvatar}
            />
          </label>
        </div>
        <div className="form-field">
          <label htmlFor="edit-display-name">表示名</label>
          <input
            id="edit-display-name"
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            maxLength={DISPLAY_NAME_MAX}
            required
          />
        </div>
        <div className="form-field">
          <label htmlFor="edit-bio">自己紹介</label>
          <textarea
            id="edit-bio"
            rows={3}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            maxLength={200}
          />
          <div className="field-meta">
            <span></span>
            <span className={`char-counter ${bio.length > BIO_MAX ? 'is-over' : ''}`}>
              {bio.length} / {BIO_MAX}
            </span>
          </div>
        </div>
        {error && <p className="error-text">{error}</p>}
        <button
          type="button"
          className="btn btn--primary"
          onClick={handleSave}
          disabled={saving || uploadingAvatar || !canSave}
        >
          保存
        </button>
      </div>
    </div>
  )
}
