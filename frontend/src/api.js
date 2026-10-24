import axios from 'axios'

export const TOKEN_KEY = 'relay.token'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    // an expired or invalid token on a protected call sends the user back to sign in
    if (err.response?.status === 401 && !err.config.url.startsWith('/auth/login')) {
      localStorage.removeItem(TOKEN_KEY)
      if (location.pathname !== '/login') location.assign('/login')
    }
    return Promise.reject(err)
  },
)

export const errorMessage = (err) =>
  err.response?.data?.message || (err.code === 'ERR_NETWORK' ? 'Cannot reach the Relay server' : err.message || 'Something went wrong')

export default api
