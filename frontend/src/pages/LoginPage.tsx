import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { setUser } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSubmitting(true)
    try {
      const user = await login({ email, password })
      setUser(user)
      navigate('/')
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : '通信エラーが発生しました')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-screen">
      <h1 className="auth-title">おかえりなさい</h1>
      <p className="auth-subtitle">RaiseSNSにログイン</p>
      {formError && <div className="form-error">{formError}</div>}
      <form onSubmit={handleSubmit}>
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
        </div>
        <div className="form-field">
          <label htmlFor="password">パスワード</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button type="submit" className="btn btn--primary btn--full" disabled={submitting}>
          ログイン
        </button>
      </form>
      <p className="auth-footer">
        アカウントをお持ちでない方は <Link to="/signup">会員登録</Link>
      </p>
    </div>
  )
}
