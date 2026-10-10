import { useState, type FormEvent } from 'react'
import AuthCard from '../components/AuthCard'
import { forgotPassword } from '../api/auth'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (loading) return
    setError(''); setMessage(''); setLoading(true)
    try { setMessage((await forgotPassword(email)).message) }
    catch (err) { setError(err instanceof Error ? err.message : 'No se pudo solicitar el enlace.') }
    finally { setLoading(false) }
  }
  return (
    <AuthCard title="Recuperar contraseña" description="Escribe el correo registrado en tu cuenta para solicitar un enlace.">
      {error && <div className="login-error" role="alert">{error}</div>}
      {message && <div className="auth-success" role="status">{message} Revisa también la carpeta de spam.</div>}
      <form onSubmit={submit} aria-busy={loading}>
        <div className="form-group">
          <label htmlFor="recovery-email">Correo electrónico</label>
          <input id="recovery-email" type="email" autoComplete="email" required maxLength={180}
            value={email} onChange={event => setEmail(event.target.value)} disabled={loading} autoFocus />
        </div>
        <button className="btn-primary auth-submit" disabled={loading}>
          {loading ? 'Solicitando…' : 'Enviar enlace de recuperación'}
        </button>
      </form>
    </AuthCard>
  )
}

