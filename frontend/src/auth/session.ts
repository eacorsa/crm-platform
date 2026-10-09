export const TOKEN_KEY = 'crm_token'
export const USER_KEY = 'crm_user'
export const SESSION_EXPIRED_EVENT = 'crm:session-expired'

// Client-side expiry is for UX only; the server still verifies the JWT signature.
export function tokenExpiresAt(token: string | null): number | null {
  if (!token) return null
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const encoded = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const { exp } = JSON.parse(atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '=')))
    return typeof exp === 'number' && Number.isFinite(exp) && exp > 0 ? exp * 1000 : null
  } catch { return null }
}

export function isTokenCurrent(token: string | null, now = Date.now()): boolean {
  const expiresAt = tokenExpiresAt(token)
  return expiresAt !== null && expiresAt > now
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
