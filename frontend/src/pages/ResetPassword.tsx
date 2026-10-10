import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import { resetPassword } from '../api/auth'
import { passwordResetError, readResetToken } from '../auth/passwordReset'
import { useAuth } from '../context/AuthContext'

export default function ResetPassword() {
  const location = useLocation()
  const token = readResetToken(location.hash)
  const { signOut } = useAuth()
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (loading || !token) return
    const validation = passwordResetError(password, confirmation)
    if (validation) { setError(validation); return }
    setError(''); setLoading(true)
    try {
      setMessage((await resetPassword(token, password)).message)
      setPassword(''); setConfirmation(''); signOut()
      window.history.replaceState(window.history.state, '', location.pathname)
    } catch (err) { setError(err instanceof Error ? err.message : 'No se pudo cambiar la contraseña.') }
    finally { setLoading(false) }
  }
  return (
    <AuthCard title="Nueva contraseña" description="Elige una contraseña de al menos 8 caracteres.">
      {message ? <div className="auth-success" role="status">{message}</div> : !token ?
        <div className="login-error" role="alert">Falta un enlace válido. <Link to="/forgot-password">Solicita uno nuevo</Link>.</div> : (
        <>
          {error && <div className="login-error" role="alert">{error} <Link to="/forgot-password">Solicitar otro enlace</Link></div>}
          <form onSubmit={submit} aria-busy={loading}>
            <div className="form-group">
              <label htmlFor="new-password">Nueva contraseña</label>
              <input id="new-password" type="password" autoComplete="new-password" required minLength={8} maxLength={72}
                value={password} onChange={event => setPassword(event.target.value)} disabled={loading} autoFocus />
            </div>
            <div className="form-group">
              <label htmlFor="confirm-password">Confirmar contraseña</label>
              <input id="confirm-password" type="password" autoComplete="new-password" required minLength={8} maxLength={72}
                value={confirmation} onChange={event => setConfirmation(event.target.value)} disabled={loading} />
            </div>
            <button className="btn-primary auth-submit" disabled={loading}>{loading ? 'Guardando…' : 'Cambiar contraseña'}</button>
          </form>
        </>
      )}
    </AuthCard>
  )
}

