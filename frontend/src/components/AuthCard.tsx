import { Link } from 'react-router-dom'
import type { ReactNode } from 'react'

export default function AuthCard({ title, description, children }: {
  title: string; description: string; children: ReactNode
}) {
  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="auth-title">
        <div className="login-logo"><span>CRM CEC</span></div>
        <h1 id="auth-title" className="login-title">{title}</h1>
        <p className="login-sub">{description}</p>
        {children}
        <p className="auth-links"><Link to="/login">Volver a iniciar sesión</Link></p>
      </section>
    </main>
  )
}

