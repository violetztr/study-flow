import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { AUTH_TOKEN_KEY } from '../api/http'

function ProtectedRoute() {
  const location = useLocation()
  const token = localStorage.getItem(AUTH_TOKEN_KEY)

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}

export default ProtectedRoute
