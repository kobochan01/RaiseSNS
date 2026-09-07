import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createComment, getComments, type Comment } from '../api/comments'
import type { Post } from '../api/posts'
import { formatRelativeTime } from '../utils/time'
import { Avatar } from './Avatar'
import { CommentItem } from './CommentItem'
import { LikeButton } from './LikeButton'
import { Modal } from './Modal'

const MAX_BODY_LENGTH = 140

type Props = {
  post: Post
  currentUserId: number
  onClose: () => void
  onUpdated: (post: Post) => void
}

function insertReply(nodes: Comment[], parentId: number, reply: Comment): Comment[] {
  return nodes.map((n) =>
    n.id === parentId ? { ...n, replies: [...n.replies, reply] } : { ...n, replies: insertReply(n.replies, parentId, reply) },
  )
}

function removeCommentFromTree(nodes: Comment[], commentId: number): Comment[] {
  return nodes.filter((n) => n.id !== commentId).map((n) => ({ ...n, replies: removeCommentFromTree(n.replies, commentId) }))
}

export function PostDetailModal({ post, currentUserId, onClose, onUpdated }: Props) {
  const [comments, setComments] = useState<Comment[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [newBody, setNewBody] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(null)
    getComments(post.id)
      .then((result) => {
        if (!cancelled) setComments(result)
      })
      .catch(() => {
        if (!cancelled) setLoadError('コメントの取得に失敗しました')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [post.id])

  function handleReplyCreated(reply: Comment, parentId: number) {
    setComments((prev) => insertReply(prev ?? [], parentId, reply))
    onUpdated({ ...post, commentCount: post.commentCount + 1 })
  }

  function handleCommentDeleted(commentId: number, newCommentCount: number) {
    setComments((prev) => removeCommentFromTree(prev ?? [], commentId))
    onUpdated({ ...post, commentCount: newCommentCount })
  }

  async function handleSubmit() {
    const body = newBody.trim()
    if (!body || body.length > MAX_BODY_LENGTH) {
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    try {
      const created = await createComment(post.id, body)
      setComments((prev) => [...(prev ?? []), created])
      onUpdated({ ...post, commentCount: post.commentCount + 1 })
      setNewBody('')
    } catch {
      setSubmitError('コメントの投稿に失敗しました')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal onClose={onClose}>
      <div className="modal-header">
        <h2>投稿の詳細</h2>
        <button type="button" className="modal-close" onClick={onClose} aria-label="閉じる">
          ×
        </button>
      </div>
      <div className="modal-body">
        <div className="post-detail__head">
          <Avatar userId={post.author.id} displayName={post.author.displayName} avatarUrl={post.author.avatarUrl} />
          <div className="post-card__head">
            <Link to={`/profile/${post.author.username}`} className="post-card__name">
              {post.author.displayName}
            </Link>
            <Link to={`/profile/${post.author.username}`} className="post-card__username">
              @{post.author.username}
            </Link>
            <span className="post-card__time">・{formatRelativeTime(post.createdAt)}</span>
          </div>
        </div>
        <p className="post-detail__text">{post.body}</p>
        {post.imageUrl && <img className="post-detail__image" src={post.imageUrl} alt="投稿画像" />}
        <div className="post-detail__footer">
          <LikeButton
            postId={post.id}
            likeCount={post.likeCount}
            isLikedByMe={post.isLikedByMe}
            onChange={(next) => onUpdated({ ...post, ...next })}
          />
        </div>

        <div className="comment-section">
          <h3>コメント({post.commentCount})</h3>
          {loading && <p className="empty-state">読み込み中...</p>}
          {loadError && <div className="form-error">{loadError}</div>}
          {!loading && comments && comments.length === 0 && (
            <div className="empty-state">まだコメントがありません。</div>
          )}
          <div className="comment-list">
            {comments?.map((comment) => (
              <CommentItem
                key={comment.id}
                comment={comment}
                postId={post.id}
                currentUserId={currentUserId}
                depth={0}
                onDeleted={handleCommentDeleted}
                onReplyCreated={handleReplyCreated}
              />
            ))}
          </div>
          <div className="comment-form">
            <textarea
              className="comment-form__textarea"
              value={newBody}
              onChange={(e) => setNewBody(e.target.value)}
              maxLength={MAX_BODY_LENGTH}
              placeholder="コメントを入力...(1〜140文字)"
            />
            <div className="field-meta">
              <span></span>
              <span className={`char-counter ${newBody.length > MAX_BODY_LENGTH ? 'is-over' : ''}`}>
                {newBody.length} / {MAX_BODY_LENGTH}
              </span>
            </div>
            {submitError && <div className="form-error">{submitError}</div>}
            <div className="post-create__footer">
              <button
                type="button"
                className="btn btn--primary btn--sm"
                onClick={handleSubmit}
                disabled={submitting || newBody.trim().length === 0 || newBody.length > MAX_BODY_LENGTH}
              >
                送信
              </button>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  )
}
