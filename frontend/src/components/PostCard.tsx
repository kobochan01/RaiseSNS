import { useState } from 'react'
import { deletePost, updatePost, type Post } from '../api/posts'
import { avatarColor } from '../utils/avatar'
import { formatRelativeTime } from '../utils/time'

const MAX_BODY_LENGTH = 280

type Props = {
  post: Post
  currentUserId: number
  onUpdated: (post: Post) => void
  onDeleted: (postId: number) => void
}

export function PostCard({ post, currentUserId, onUpdated, onDeleted }: Props) {
  const [editing, setEditing] = useState(false)
  const [editBody, setEditBody] = useState(post.body)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isOwner = post.author.id === currentUserId

  function startEdit() {
    setEditBody(post.body)
    setError(null)
    setEditing(true)
  }

  function cancelEdit() {
    setEditing(false)
    setError(null)
  }

  async function saveEdit() {
    const body = editBody.trim()
    if (!body || body.length > MAX_BODY_LENGTH) {
      return
    }
    setSaving(true)
    setError(null)
    try {
      const updated = await updatePost(post.id, { body })
      onUpdated(updated)
      setEditing(false)
    } catch {
      setError('更新に失敗しました')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!window.confirm('この投稿を削除しますか?この操作は取り消せません。')) {
      return
    }
    try {
      await deletePost(post.id)
      onDeleted(post.id)
    } catch {
      setError('削除に失敗しました')
    }
  }

  return (
    <article className="post-card">
      <div className="avatar avatar--md" style={{ backgroundColor: avatarColor(post.author.id) }}>
        {post.author.displayName.charAt(0)}
      </div>
      <div className="post-card__body">
        <div className="post-card__head">
          <span className="post-card__name">{post.author.displayName}</span>
          <span className="post-card__username">@{post.author.username}</span>
          <span className="post-card__time">・{formatRelativeTime(post.createdAt)}</span>
        </div>
        {editing ? (
          <div className="post-card__edit">
            <textarea
              className="post-card__edit-textarea"
              value={editBody}
              onChange={(e) => setEditBody(e.target.value)}
              maxLength={500}
            />
            <div className="field-meta">
              <span></span>
              <span className={`char-counter ${editBody.length > MAX_BODY_LENGTH ? 'is-over' : ''}`}>
                {editBody.length} / {MAX_BODY_LENGTH}
              </span>
            </div>
            {error && <p className="error-text">{error}</p>}
            <div className="edit-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={cancelEdit} disabled={saving}>
                キャンセル
              </button>
              <button
                type="button"
                className="btn btn--primary btn--sm"
                onClick={saveEdit}
                disabled={saving || editBody.trim().length === 0 || editBody.length > MAX_BODY_LENGTH}
              >
                保存
              </button>
            </div>
          </div>
        ) : (
          <p className="post-card__text">{post.body}</p>
        )}
        {isOwner && !editing && (
          <div className="post-card__owner-actions">
            <button type="button" className="btn-link" onClick={startEdit}>
              編集
            </button>
            <button type="button" className="btn-link" onClick={handleDelete}>
              削除
            </button>
            {error && <p className="error-text">{error}</p>}
          </div>
        )}
      </div>
    </article>
  )
}
