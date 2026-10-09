import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'
import { TOKEN_KEY, USER_KEY, SESSION_EXPIRED_EVENT, clearSession, isTokenCurrent, tokenExpiresAt } from '../auth/session'
import type { AuthResponse } from '../types'

interface AuthState {
  token: string | null
  user:  Omit<AuthResponse, 'token'> | null
}

interface AuthContextValue extends AuthState {
  signIn:  (data: AuthResponse) => void
  signOut: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadInitialState(): AuthState {
  const token = localStorage.getItem(TOKEN_KEY)
  const raw   = localStorage.getItem(USER_KEY)
  if (!isTokenCurrent(token) || !raw) {
    clearSession()
    return { token: null, user: null }
  }
  try {
    const user = JSON.parse(raw)
    if (typeof user?.name !== 'string' || typeof user?.email !== 'string' ||
        !['ADMIN', 'AGENT'].includes(user?.role)) throw new Error('Invalid stored user')
    return { token, user }
  } catch {
    clearSession()
    return { token: null, user: null }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadInitialState)

  const signIn = useCallback((data: AuthResponse) => {
    const { token, ...user } = data
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY,  JSON.stringify(user))
    setState({ token, user })
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    setState({ token: null, user: null })
  }, [])

  useEffect(() => {
    const syncSession = () => setState(loadInitialState())
    window.addEventListener(SESSION_EXPIRED_EVENT, signOut)
    window.addEventListener('storage', syncSession)
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, signOut)
      window.removeEventListener('storage', syncSession)
    }
  }, [signOut])

  useEffect(() => {
    if (!state.token) return
    let timer: ReturnType<typeof setTimeout>
    const checkExpiry = () => {
      const remaining = (tokenExpiresAt(state.token) ?? 0) - Date.now()
      if (remaining <= 0) signOut()
      else timer = setTimeout(checkExpiry, Math.min(remaining, 2_147_483_647))
    }
    checkExpiry()
    return () => clearTimeout(timer)
  }, [state.token, signOut])

  return (
    <AuthContext.Provider value={{ ...state, signIn, signOut, isAuthenticated: isTokenCurrent(state.token) }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
