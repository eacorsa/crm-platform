export function readResetToken(hash: string): string {
  const token = new URLSearchParams(hash.replace(/^#/, '')).get('token') ?? ''
  return /^[A-Za-z0-9_-]{43}$/.test(token) ? token : ''
}

export function passwordResetError(password: string, confirmation: string): string | null {
  if (password.trim().length === 0 || password.length < 8 || new TextEncoder().encode(password).length > 72)
    return 'La contraseña debe tener al menos 8 caracteres y no superar 72 bytes UTF-8.'
  if (password !== confirmation) return 'Las contraseñas no coinciden.'
  return null
}

