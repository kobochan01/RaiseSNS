import { useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import { useAuth } from '../context/AuthContext'

export function HomePage() {
  const { user, setUser } = useAuth()
  const navigate = useNavigate()

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
    <div className="home-screen">
      <h1 className="auth-title">ログイン成功</h1>
      <p className="auth-subtitle">ようこそ、{user?.displayName}さん</p>
      <button type="button" className="btn btn--outline btn--full" onClick={handleLogout}>
        ログアウト
      </button>
    </div>
  )
}
