import axios from 'axios'
import type { ApiResponse } from '@/types'

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function unwrap<T>(
  promise: Promise<import('axios').AxiosResponse<ApiResponse<T>>>
): Promise<T> {
  const resp = await promise
  const body = resp.data
  if (body.code !== 0) {
    throw new Error(body.message ?? `biz error code=${body.code}`)
  }
  return body.data
}
