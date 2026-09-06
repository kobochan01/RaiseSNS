import { useState, type CSSProperties } from 'react'
import { createComment, deleteComment, type Comment } from '../api/comments'
import { avatarColor } from '../utils/avatar'
import { formatRelativeTime } from '../utils/time'

const MAX_BODY_LENGTH = 140

type Props = {
  comment: Comment
  postId: number
  currentUserId: number
  depth: number
  onDeleted: (commentId: number, newCommentCount: number) => void
  onReplyCreated: (reply: Comment, parentId: number) => void
}

export function CommentItem({ comment, postId, currentUserId, depth, onDeleted, onReplyCreated }: Props) {
  const [replying, setReplying] = useState(false)
  const [replyBody, setReplyBody] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const isOwner = comment.author.id === currentUserId

  async function handleDelete() {
    if (!window.confirm('このコメントを削除しますか?')) {
      return
    }
    try {
      const response = await deleteComment(comment.id)
      onDeleted(comment.id, response.commentCount)
    } catch {
      setError('削除に失敗しました')
    }
  }

  async function handleReplySubmit() {
    const body = replyBody.trim()
    if (!body || body.length > MAX_BODY_LENGTH) {
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const reply = await createComment(postId, body, comment.id)
      onReplyCreated(reply, comment.id)
      setReplyBody('')
      setReplying(false)
    } catch {
      setError('返信に失敗しました')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="comment-item" style={{ '--depth': depth } as CSSProperties}>
      <div className="avatar avatar--sm" style={{ backgroundColor: avatarColor(comment.author.id) }}>
        {comment.author.displayName.charAt(0)}
      </div>
      <div className="comment-item__body">
        <div className="comment-item__head">
          <span className="comment-item__name">{comment.author.displayName}</span>
          <span className="comment-item__username">@{comment.author.username}</span>
          <span className="comment-item__time">・{formatRelativeTime(comment.createdAt)}</span>
        </div>
        <p className="comment-item__text">{comment.body}</p>
        <div className="comment-item__actions">
          <button type="button" className="btn-link" onClick={() => setReplying((v) => !v)}>
            返信
          </button>
          {isOwner && (
            <button type="button" className="btn-link" onClick={handleDelete}>
              削除
            </button>
          )}
        </div>
        {error && <p className="error-text">{error}</p>}
        {replying && (
          <div className="comment-form">
            <textarea
              className="comment-form__textarea"
              value={replyBody}
              onChange={(e) => setReplyBody(e.target.value)}
              maxLength={MAX_BODY_LENGTH}
              placeholder="返信を入力...(1〜140文字)"
            />
            <div className="field-meta">
              <span></span>
              <span className={`char-counter ${replyBody.length > MAX_BODY_LENGTH ? 'is-over' : ''}`}>
                {replyBody.length} / {MAX_BODY_LENGTH}
              </span>
            </div>
            <div className="edit-actions">
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={() => setReplying(false)}
                disabled={submitting}
              >
                キャンセル
              </button>
              <button
                type="button"
                className="btn btn--primary btn--sm"
                onClick={handleReplySubmit}
                disabled={submitting || replyBody.trim().length === 0 || replyBody.length > MAX_BODY_LENGTH}
              >
                送信
              </button>
            </div>
          </div>
        )}
        {comment.replies.map((reply) => (
          <CommentItem
            key={reply.id}
            comment={reply}
            postId={postId}
            currentUserId={currentUserId}
            depth={depth + 1}
            onDeleted={onDeleted}
            onReplyCreated={onReplyCreated}
          />
        ))}
      </div>
    </div>
  )
}
