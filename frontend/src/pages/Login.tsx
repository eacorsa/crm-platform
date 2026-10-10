import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { login } from '../api/auth'

export default function Login() {
  const { signIn } = useAuth()
  const navigate   = useNavigate()
  const [email, setEmail]       = useState('')
  const [password, setPassword] = useState('')
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await login(email, password)
      signIn(data)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al iniciar sesión')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <span>CRM CEC</span>
        </div>
        <h1 className="login-title">Bienvenido</h1>
        <p className="login-sub">Accede a tu panel de CRM</p>

        {error && <div className="login-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Correo electrónico</label>
            <input
              id="email" type="email" required autoFocus
              placeholder="admin@crmclientes.mx"
              value={email} onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="form-group" style={{ marginBottom: 24 }}>
            <label htmlFor="password">Contraseña</label>
            <input
              id="password" type="password" required
              placeholder="••••••••"
              value={password} onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary" disabled={loading} style={{ width: '100%', padding: 12 }}>
            {loading ? 'Iniciando sesión…' : 'Iniciar sesión'}
          </button>
        </form>

        <p className="auth-links"><Link to="/forgot-password">¿Olvidaste tu contraseña?</Link></p>

        <p className="login-footer">CRM Clientes © {new Date().getFullYear()}</p>
      </div>
    </div>
  )
}
