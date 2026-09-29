import { http, unwrap } from './http'
import type { ApiResponse, Demand, Page, Order, LogisticsEvent, Notice, Review, RouteTask, Payment } from '@/types'

export const userApi = {
  publishDemand(body: {
    title: string; weightG: number; volumeCm3: number; fragile: boolean
    originRegion: string; originAddr: string; targetRegion: string; targetAddr: string
  }): Promise<Demand> {
    return unwrap(http.post<ApiResponse<Demand>>('/user/demands', body))
  },
  myDemands(status?: string, page = 0, size = 20): Promise<Page<Demand>> {
    return unwrap(http.get<ApiResponse<Page<Demand>>>('/user/demands', { params: { status, page, size } }))
  },
  myOrders(status?: string, page = 0, size = 20): Promise<Page<Order>> {
    return unwrap(http.get<ApiResponse<Page<Order>>>('/user/orders', { params: { status, page, size } }))
  },
  orderLogistics(orderId: number): Promise<LogisticsEvent[]> {
    return unwrap(http.get<ApiResponse<LogisticsEvent[]>>(`/user/orders/${orderId}/logistics`))
  },
  latestRoute(orderId: number): Promise<RouteTask> {
    return unwrap(http.get<ApiResponse<RouteTask>>(`/user/orders/${orderId}/route`))
  },
  confirmDemand(demandId: number): Promise<Order> {
    return unwrap(http.post<ApiResponse<Order>>(`/user/demands/${demandId}/confirm`))
  },
  pay(orderId: number): Promise<Payment> {
    return unwrap(http.post<ApiResponse<Payment>>(`/user/orders/${orderId}/pay`))
  },
  payCallback(orderId: number, txnNo: string, amountFen: number): Promise<Payment> {
    return unwrap(http.post<ApiResponse<Payment>>(`/user/orders/${orderId}/pay/callback`, { txnNo, amountFen }))
  },
  submitReview(orderId: number, rating: number, content: string): Promise<Review> {
    return unwrap(http.post<ApiResponse<Review>>(`/user/orders/${orderId}/review`, { rating, content }))
  },
  submitFeedback(type: string, content: string): Promise<{ id: number }> {
    return unwrap(http.post<ApiResponse<{ id: number }>>('/user/feedback', { type, content }))
  },
  notices(): Promise<Notice[]> {
    return unwrap(http.get<ApiResponse<Notice[]>>('/notices'))
  },
  sites(): Promise<{ id: number; name: string }[]> {
    return unwrap(http.get<ApiResponse<{ id: number; name: string }[]>>('/user/sites'))
  },
  submitStaffApplication(body: { siteId: number; licenseNo: string; reason?: string }): Promise<StaffApplicationDto> {
    return unwrap(http.post<ApiResponse<StaffApplicationDto>>('/user/staff-application', body))
  },
  myStaffApplication(): Promise<StaffApplicationDto | null> {
    return unwrap(http.get<ApiResponse<StaffApplicationDto | null>>('/user/staff-application'))
  },
  cancelStaffApplication(): Promise<void> {
    return unwrap(http.delete<ApiResponse<void>>('/user/staff-application'))
  },
  openIm(): Promise<{ id: number }> {
    return unwrap(http.post<ApiResponse<{ id: number }>>('/user/im/sessions', {}))
  },
  imHistory(sessionId: number): Promise<{ id: number; senderRole: string; content: string; sentAt: string }[]> {
    return unwrap(http.get<ApiResponse<{ id: number; senderRole: string; content: string; sentAt: string }[]>>(`/user/im/sessions/${sessionId}/messages`))
  },
  sendIm(sessionId: number, content: string): Promise<{ id: number }> {
    return unwrap(http.post<ApiResponse<{ id: number }>>(`/user/im/sessions/${sessionId}/messages`, { content }))
  },
}

export interface StaffApplicationDto {
  id?: number
  userId: number
  siteId: number
  licenseNo: string
  reason?: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  adminId?: number | null
  decidedAt?: string | null
  createdAt: string
}
