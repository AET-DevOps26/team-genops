import { useEffect } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAppDispatch } from '~/store/hooks'
import { fetchMe } from '~/services/auth/authSlice'
import { ProtectedRoute } from '~/components/routing/ProtectedRoute'
import { PublicOnlyRoute } from '~/components/routing/PublicOnlyRoute'
import AuthPage from '~/pages/AuthPage'
import BoardPage from '~/pages/BoardPage'

function App() {
  const dispatch = useAppDispatch()

  // Session bootstrap: ask /me once on mount. Until it resolves the gates show a Splash
  // (status === 'unknown'), so a refreshing user is never bounced to /login prematurely.
  useEffect(() => {
    dispatch(fetchMe())
  }, [dispatch])

  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<AuthPage />} />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<BoardPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
