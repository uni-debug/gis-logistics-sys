import { http, unwrap } from './http'
import type { ApiResponse, LoginResponse, Review } from '@/types'

export const authApi = {
  register(phone: string, name: string, password: string) {
    return unwrap<number>(http.post<ApiResponse<number>>('auth/register', { phone, name, password }))
  },
  login(phone: string, password: string, expectedRole?: string): Promise<LoginResponse> {
    return unwrap<LoginResponse>(http.post<ApiResponse<LoginResponse>>('auth/login', { phone, password, expectedRole }))
  },
  me() {
    return unwrap<{ id: number; name: string; role: string }>(
      http.get<ApiResponse<{ id: number; name: string; role: string }>>('auth/me')
    )
  },
  profile(): Promise<{ id: number; name: string; role: string; phoneMasked: string }> {
    return unwrap<{ id: number; name: string; role: string; phoneMasked: string }>(
      http.get<ApiResponse<{ id: number; name: string; role: string; phoneMasked: string }>>('user/profile')
    )
  },
  updateProfile(name: string): Promise<{ id: number; name: string }> {
    return unwrap<{ id: number; name: string }>(
      http.patch<ApiResponse<{ id: number; name: string }>>('user/profile', { name })
    )
  },
  changePassword(oldPassword: string, newPassword: string): Promise<{ changed: boolean }> {
    return unwrap<{ changed: boolean }>(
      http.post<ApiResponse<{ changed: boolean }>>('user/password', { oldPassword, newPassword })
    )
  },
  myReviews(): Promise<Review[]> {
    return unwrap<Review[]>(http.get<ApiResponse<Review[]>>('user/reviews'))
  },
  myFeedbacks(): Promise<{ id: number; type: string; content: string; status: string; reply: string | null }[]> {
    return unwrap<{ id: number; type: string; content: string; status: string; reply: string | null }[]>(
      http.get<ApiResponse<{ id: number; type: string; content: string; status: string; reply: string | null }[]>>(
        'user/feedbacks'
      )
    )
  },
}
