// PUT replaces editable fields; empty optional values explicitly clear stored data.
export const optionalText = (value: string): string | null => value.trim() || null
export const optionalDate = (value: string): string | null => value || null
export const optionalAmount = (value: string): number | null => value === '' ? null : Number(value)
