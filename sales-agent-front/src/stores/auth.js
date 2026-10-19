import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('sa-token') || '')
  const userInfo = ref((() => {
    try {
      const raw = localStorage.getItem('user-info')
      return raw && raw !== 'undefined' ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })())

  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  async function login(repId, password) {
    // 后端返回 { token, username, role }，直接映射
    const res = await authApi.login({ repId })
    const data = res.data
    token.value = data.token
    userInfo.value = {
      username: data.username,
      role: data.role,
    }
    localStorage.setItem('sa-token', token.value)
    localStorage.setItem('user-info', JSON.stringify(userInfo.value))
    return data
  }

  // 清除登录态（不调后端），供 401 拦截器使用
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('sa-token')
    localStorage.removeItem('user-info')
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
      const { default: router } = await import('@/router')
      router.push('/login')
    }
  }

  return { token, userInfo, isLoggedIn, login, logout, clearAuth }
})
