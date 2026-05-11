import { NavLink, Link, useMatch, useSearchParams } from 'react-router-dom'
import { getSeasonTabsForRole } from '../../constants/seasonNav.js'

export function AuthenticatedLayout({ auth, onLogout, children }) {
  const seasonMatch = useMatch('/seasons/:seasonId')
  const seasonId = seasonMatch?.params?.seasonId
  const [searchParams] = useSearchParams()
  const activeTab = searchParams.get('tab') || 'summary'
  const seasonTabs = seasonId ? getSeasonTabsForRole(auth.role) : []

  return (
    <div className="layout-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-badge">$</div>
          <div>
            <h1>Budget Buddy</h1>
            <p>Finance workspace</p>
          </div>
        </div>
        <nav className="sidebar-nav">
          <NavLink end to="/seasons" className={({ isActive }) => (isActive ? 'active' : '')}>
            Seasons
          </NavLink>
          {seasonId && (
            <div className="sidebar-season-block">
              <div className="sidebar-season-label">Season</div>
              <div className="sidebar-season-nav">
                {seasonTabs.map((tab) => (
                  <Link
                    key={tab.id}
                    to={`/seasons/${seasonId}?tab=${tab.id}`}
                    className={activeTab === tab.id ? 'active' : ''}
                  >
                    {tab.label}
                  </Link>
                ))}
              </div>
            </div>
          )}
          {auth.role === 'ADMIN' && (
            <NavLink to="/users" className={({ isActive }) => (isActive ? 'active' : '')}>
              Users
            </NavLink>
          )}
          {auth.role === 'ADMIN' && (
            <NavLink to="/teams" className={({ isActive }) => (isActive ? 'active' : '')}>
              Teams
            </NavLink>
          )}
          {auth.role === 'ADMIN' && (
            <NavLink to="/audit-log" className={({ isActive }) => (isActive ? 'active' : '')}>
              Audit log
            </NavLink>
          )}
        </nav>
        <div className="sidebar-footer">
          <p>{auth.username}</p>
          <p>{auth.role}</p>
          <button type="button" className="btn-logout" onClick={onLogout}>
            Logout
          </button>
        </div>
      </aside>
      <div className="content-shell">
        <main className="app-main">{children}</main>
      </div>
    </div>
  )
}
