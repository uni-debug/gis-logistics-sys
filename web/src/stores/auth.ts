import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { Role } from '@/types'

interface AuthState {
  token: string | null
  role: Role | null
  userId: number | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    role: (localStorage.getItem('role') as Role | null),
    userId: localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    async login(phone: string, password: string, expectedRole?: string) {
      const resp = await authApi.login(phone, password, expectedRole)
      this.token = resp.token
      this.role = resp.role
      this.userId = resp.userId
      localStorage.setItem('token', resp.token)
      localStorage.setItem('role', resp.role)
      localStorage.setItem('userId', String(resp.userId))
      return resp
    },
    logout() {
      this.token = null
      this.role = null
      this.userId = null
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('userId')
    },
  },
})
