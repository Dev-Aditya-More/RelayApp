import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api, { TOKEN_KEY } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!localStorage.getItem(TOKEN_KEY))

  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return
    api.get('/auth/me')
      .then((r) => setUser(r.data))
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const finish = useCallback((res) => {
    localStorage.setItem(TOKEN_KEY, res.data.token)
    setUser(res.data.user)
    return res.data.user
  }, [])

  const value = useMemo(() => ({
    user,
    loading,
    login: (email, password) => api.post('/auth/login', { email, password }).then(finish),
    register: (body) => api.post('/auth/register', body).then(finish),
    refresh: () => api.get('/auth/me').then((r) => setUser(r.data)).catch(() => {}),
    logout: () => { localStorage.removeItem(TOKEN_KEY); setUser(null) },
  }), [user, loading, finish])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
