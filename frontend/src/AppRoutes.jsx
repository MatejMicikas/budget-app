import { useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthenticatedLayout } from './components/layout/AuthenticatedLayout.jsx'
import { AuditLogPage } from './pages/AuditLogPage.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { SeasonDetailPage } from './pages/SeasonDetailPage.jsx'
import { SeasonsPage } from './pages/SeasonsPage.jsx'
import { UsersPage } from './pages/UsersPage.jsx'
import { TeamsPage } from './pages/TeamsPage.jsx'

const STORAGE_KEY = 'budget-app-auth'

export function AppRoutes() {
  const [auth, setAuth] = useState(() => {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  })

  const onLogin = (jwtResponse) => {
    const data = {
      token: jwtResponse.token,
      userId: jwtResponse.userId,
      username: jwtResponse.username,
      role: jwtResponse.role,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    setAuth(data)
  }

  const onLogout = () => {
    localStorage.removeItem(STORAGE_KEY)
    setAuth(null)
  }

  if (!auth) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage onLogin={onLogin} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <AuthenticatedLayout auth={auth} onLogout={onLogout}>
      <Routes>
        <Route path="/seasons" element={<SeasonsPage auth={auth} />} />
        <Route path="/seasons/:seasonId" element={<SeasonDetailPage auth={auth} />} />
        <Route
          path="/users"
          element={auth.role === 'ADMIN' ? <UsersPage auth={auth} /> : <Navigate to="/seasons" replace />}
        />
        <Route
          path="/teams"
          element={auth.role === 'ADMIN' ? <TeamsPage auth={auth} /> : <Navigate to="/seasons" replace />}
        />
        <Route
          path="/audit-log"
          element={auth.role === 'ADMIN' ? <AuditLogPage auth={auth} /> : <Navigate to="/seasons" replace />}
        />
        <Route path="*" element={<Navigate to="/seasons" replace />} />
      </Routes>
    </AuthenticatedLayout>
  )
}
