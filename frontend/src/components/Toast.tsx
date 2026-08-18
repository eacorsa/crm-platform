import { useEffect } from 'react'

interface Props {
  message: string
  type?:   'error' | 'success'
  onDone:  () => void
}

export default function Toast({ message, type = 'error', onDone }: Props) {
  useEffect(() => {
    const t = setTimeout(onDone, 4000)
    return () => clearTimeout(t)
  }, [onDone])

  return <div className={`toast ${type}`}>{message}</div>
}
