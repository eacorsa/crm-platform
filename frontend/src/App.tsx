import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Login    from './pages/Login'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import Layout   from './pages/Layout'
import Contacts  from './pages/Contacts'
import Companies from './pages/Companies'
import Deals     from './pages/Deals'
import Tasks     from './pages/Tasks'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route index               element={<Navigate to="/contacts" replace />} />
        <Route path="contacts"     element={<Contacts />} />
        <Route path="companies"    element={<Companies />} />
        <Route path="deals"        element={<Deals />} />
        <Route path="tasks"        element={<Tasks />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
