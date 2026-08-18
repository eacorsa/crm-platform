import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
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

const TOKEN_KEY = 'crm_token'
const USER_KEY  = 'crm_user'

function loadInitialState(): AuthState {
  const token = localStorage.getItem(TOKEN_KEY)
  const raw   = localStorage.getItem(USER_KEY)
  if (!token || !raw) return { token: null, user: null }
  try {
    return { token, user: JSON.parse(raw) }
  } catch {
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
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setState({ token: null, user: null })
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, signIn, signOut, isAuthenticated: !!state.token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
