import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/',
  timeout: 60000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('sa-token')
  if (token) config.headers['sa-token'] = token
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (err) => {
    const status = err.response?.status
    if (status === 401) {
      // 先清 Pinia 内存状态，再跳转，否则路由守卫仍认为已登录
      const { useAuthStore } = await import('@/stores/auth')
      useAuthStore().clearAuth()
      const { default: router } = await import('@/router')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else if (err.response?.data?.message) {
      ElMessage.error(err.response.data.message)
    } else {
      ElMessage.error('网络请求失败')
    }
    return Promise.reject(err)
  }
)

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
}

export const agentApi = {
  chat: (data) => http.post('/agent/chat', data),
  clearSession: (sessionId) => http.delete(`/agent/session/${sessionId}`),
}

export default http
