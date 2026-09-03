import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, register } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'

const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,30}$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type Fields = { username: string; displayName: string; email: string; password: string }

function validate(fields: Fields): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!USERNAME_PATTERN.test(fields.username)) {
    errors.username = 'ユーザー名は3〜30文字の英数字とアンダースコアのみ使用できます。'
  }
  if (fields.displayName.length < 1 || fields.displayName.length > 50) {
    errors.displayName = '表示名は1〜50文字で入力してください。'
  }
  if (!EMAIL_PATTERN.test(fields.email)) {
    errors.email = 'メールアドレスの形式が正しくありません。'
  }
  if (fields.password.length < 8) {
    errors.password = 'パスワードは8文字以上で入力してください。'
  }
  return errors
}

export function SignupPage() {
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { setUser } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)

    const errors = validate({ username, displayName, email, password })
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      return
    }

    setSubmitting(true)
    try {
      await register({ username, email, password, displayName })
      const user = await login({ email, password })
      setUser(user)
      navigate('/')
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setFormError('このユーザー名またはメールアドレスは既に使用されています。')
        } else if (err.fieldErrors) {
          setFieldErrors(err.fieldErrors)
        } else {
          setFormError(err.message)
        }
      } else {
        setFormError('通信エラーが発生しました')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-screen">
      <h1 className="auth-title">アカウント作成</h1>
      <p className="auth-subtitle">RaiseSNSに会員登録</p>
      {formError && <div className="form-error">{formError}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-field">
          <label htmlFor="username">ユーザー名（@username）</label>
          <input
            id="username"
            type="text"
            placeholder="例: taro_dev"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          {fieldErrors.username && <p className="error-text">{fieldErrors.username}</p>}
        </div>
        <div className="form-field">
          <label htmlFor="displayName">表示名</label>
          <input
            id="displayName"
            type="text"
            placeholder="例: 太郎"
            required
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
          {fieldErrors.displayName && <p className="error-text">{fieldErrors.displayName}</p>}
        </div>
        <div className="form-field">
          <label htmlFor="email">メールアドレス</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {fieldErrors.email && <p className="error-text">{fieldErrors.email}</p>}
        </div>
        <div className="form-field">
          <label htmlFor="password">パスワード（8文字以上）</label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {fieldErrors.password && <p className="error-text">{fieldErrors.password}</p>}
        </div>
        <button type="submit" className="btn btn--primary btn--full" disabled={submitting}>
          登録する
        </button>
      </form>
      <p className="auth-footer">
        既にアカウントをお持ちの方は <Link to="/login">ログイン</Link>
      </p>
    </div>
  )
}
