import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { refresh } from './api/auth'
import { ProtectedRoute } from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { TimelinePage } from './pages/TimelinePage'

export function App() {
  const { setUser } = useAuth()
  const [initializing, setInitializing] = useState(true)

  useEffect(() => {
    refresh()
      .then(setUser)
      .catch(() => {})
      .finally(() => setInitializing(false))
  }, [setUser])

  if (initializing) {
    return null
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <TimelinePage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
