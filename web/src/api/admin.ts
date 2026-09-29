import { http, unwrap } from './http'
import type { ApiResponse, Demand, Page, Order, OrderStatus } from '@/types'

export interface StatsSnapshot {
  monthly: { month: string; orderCount: number; totalAmountFen: number }[]
  byRegion: { region: string; orderCount: number }[]
  topRoutes: Record<string, number>
}
export interface NoticeDto {
  id: number
  title: string
  body: string | null
  status: number
  pinned: boolean
  createdAt: string
}
export interface ReviewDto {
  id: number
  orderId: number
  userId: number
  staffId: number
  rating: number
  content: string | null
  staffReply: string | null
  deleted: boolean
  createdAt: string
}
export interface FeedbackDto {
  id: number
  userId: number
  type: string
  content: string
  status: string
  reply: string | null
  createdAt: string
}
export interface ImSessionDto {
  id: number
  userId: number
  status: string
  adminId: number | null
  createdAt: string
}
export interface ImMessageDto {
  id: number
  sessionId: number
  senderRole: string
  content: string
  sentAt: string
}
export interface UserDto {
  id: number
  name: string | null
  avatar: string | null
  role: string
  status: number
  createdAt: string
}
export interface StaffDto {
  id: number
  userId: number
  siteId: number | null
  licenseNo: string | null
  deliveryArea: unknown
  status: number
}
export interface StaffApplicationDto {
  id: number
  userId: number
  siteId: number
  licenseNo: string
  reason?: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  adminId?: number | null
  decidedAt?: string | null
  createdAt: string
}

export interface AddStaffBody {
  userId: number
  siteId?: number | null
  licenseNo?: string
}

export interface WarehouseSiteDto {
  id: number
  code: string
  name: string
  capacity: number
  status: number
}

export interface WarehouseRecordDto {
  id: number
  siteId: number
  orderId: number | null
  action: string
  quantity: number
  occurredAt: string
}

export interface RouteDto {
  taskId: number
  orderId: number
  strategy: string
  fromRegion: string
  toRegion: string
  fromLon: number
  fromLat: number
  toLon: number
  toLat: number
  distanceKm: number
  etaSeconds: number
  costFen: number
  createdAt: string
  geometry: number[][]
  via?: number[][]
}
export interface RouteSummary {
  total: number
  avgDistanceKm: number
  avgEtaSeconds: number
  strategyDistribution: Record<string, number>
}
export interface RouteSnapshot {
  summary: RouteSummary
  topByDistance: RouteDto[]
  recent: RouteDto[]
}
export const adminApi = {
  orders(status?: OrderStatus, staffId?: number, userId?: number, from?: string, to?: string, page = 0, size = 20): Promise<Page<Order>> {
    return unwrap(http.get<ApiResponse<Page<Order>>>('/admin/orders', { params: { status, staffId, userId, from, to, page, size } }))
  },
  orderStats(months = 12): Promise<StatsSnapshot> {
    return unwrap(http.get<ApiResponse<StatsSnapshot>>('/admin/orders/stats', { params: { months } }))
  },
  users(page = 0, size = 20): Promise<Page<UserDto>> {
    return unwrap(http.get<ApiResponse<Page<UserDto>>>('/admin/users', { params: { page, size } }))
  },
  setUserStatus(id: number, status: number): Promise<UserDto> {
    return unwrap(http.patch<ApiResponse<UserDto>>(`/admin/users/${id}/status`, { status }))
  },
  staff(): Promise<StaffDto[]> {
    return unwrap(http.get<ApiResponse<StaffDto[]>>('/admin/staff'))
  },
  setStaffStatus(id: number, status: number): Promise<StaffDto> {
    return unwrap(http.patch<ApiResponse<StaffDto>>(`/admin/staff/${id}/status`, { status }))
  },
    staffApplications(status?: string): Promise<StaffApplicationDto[]> {
      return unwrap(http.get<ApiResponse<StaffApplicationDto[]>>('/admin/staff-applications', { params: { status } }))
    },
    approveApplication(id: number): Promise<StaffApplicationDto> {
      return unwrap(http.post<ApiResponse<StaffApplicationDto>>(`/admin/staff-applications/${id}/approve`, {}))
    },
    rejectApplication(id: number, reason?: string): Promise<StaffApplicationDto> {
      return unwrap(http.post<ApiResponse<StaffApplicationDto>>(`/admin/staff-applications/${id}/reject`, { reason }))
    },
  addStaff(cmd: AddStaffBody): Promise<StaffDto> {
    return unwrap(http.post<ApiResponse<StaffDto>>('/admin/staff', cmd))
  },
  notices(status?: number, page = 0, size = 20): Promise<Page<NoticeDto>> {
    return unwrap(http.get<ApiResponse<Page<NoticeDto>>>('/admin/notices', { params: { status, page, size } }))
  },
  createNotice(title: string, body: string): Promise<NoticeDto> {
    return unwrap(http.post<ApiResponse<NoticeDto>>('/admin/notices', { title, body }))
  },
  publishNotice(id: number): Promise<NoticeDto> {
    return unwrap(http.post<ApiResponse<NoticeDto>>(`/admin/notices/${id}/publish`))
  },
  archiveNotice(id: number): Promise<NoticeDto> {
    return unwrap(http.post<ApiResponse<NoticeDto>>(`/admin/notices/${id}/archive`))
  },
  pinNotice(id: number, pinned: boolean): Promise<NoticeDto> {
    return unwrap(http.patch<ApiResponse<NoticeDto>>(`/admin/notices/${id}/pin`, { pinned }))
  },
  reviews(staffId?: number): Promise<ReviewDto[]> {
    return unwrap(http.get<ApiResponse<ReviewDto[]>>('/admin/reviews', { params: { staffId } }))
  },
  deleteReview(id: number): Promise<ReviewDto> {
    return unwrap(http.post<ApiResponse<ReviewDto>>(`/admin/reviews/${id}/delete`))
  },
  approvalRate(): Promise<{ total: number; positive: number; rate: number }> {
    return unwrap(http.get<ApiResponse<{ total: number; positive: number; rate: number }>>('/admin/reviews/approval-rate'))
  },
  feedback(status?: string): Promise<FeedbackDto[]> {
    return unwrap(http.get<ApiResponse<FeedbackDto[]>>('/admin/feedback', { params: { status } }))
  },
  processFeedback(id: number, status: string): Promise<FeedbackDto> {
    return unwrap(http.post<ApiResponse<FeedbackDto>>(`/admin/feedback/${id}/process`, null, { params: { status } }))
  },
  replyFeedback(id: number, text: string): Promise<FeedbackDto> {
    return unwrap(http.post<ApiResponse<FeedbackDto>>(`/admin/feedback/${id}/reply`, { text }))
  },
  imSessions(): Promise<ImSessionDto[]> {
    return unwrap(http.get<ApiResponse<ImSessionDto[]>>('/admin/im/sessions'))
  },
  imMessages(sessionId: number): Promise<ImMessageDto[]> {
    return unwrap(http.get<ApiResponse<ImMessageDto[]>>(`/admin/im/sessions/${sessionId}/messages`))
  },
  imAssign(sessionId: number, adminId: number): Promise<ImSessionDto> {
    return unwrap(http.post<ApiResponse<ImSessionDto>>(`/admin/im/sessions/${sessionId}/assign`, null, { params: { adminId } }))
  },
  imClose(sessionId: number): Promise<ImSessionDto> {
    return unwrap(http.post<ApiResponse<ImSessionDto>>(`/admin/im/sessions/${sessionId}/close`))
  },
  auditDemands(page = 0, size = 20): Promise<Page<Demand>> {
    return unwrap(http.get<ApiResponse<Page<Demand>>>('/admin/demands/audit', { params: { page, size } }))
  },
  closeDemand(id: number, reason?: string): Promise<Demand> {
    return unwrap(http.post<ApiResponse<Demand>>(`/admin/demands/${id}/close`, null, { params: { reason } }))
  },
  warehouseSites(): Promise<WarehouseSiteDto[]> {
    return unwrap(http.get<ApiResponse<WarehouseSiteDto[]>>('/admin/warehouse/sites'))
  },
  warehouseStock(siteId: number): Promise<number> {
    return unwrap(http.get<ApiResponse<number>>(`/admin/warehouse/${siteId}/stock`))
  },
  warehouseRecords(siteId: number): Promise<WarehouseRecordDto[]> {
    return unwrap(http.get<ApiResponse<WarehouseRecordDto[]>>(`/admin/warehouse/${siteId}/records`))
  },
  warehouseInbound(siteId: number, orderId: number | undefined, quantity: number): Promise<WarehouseRecordDto> {
    return unwrap(http.post<ApiResponse<WarehouseRecordDto>>(`/admin/warehouse/${siteId}/inbound`, { orderId, quantity }))
  },
  warehouseOutbound(siteId: number, orderId: number | undefined, quantity: number): Promise<WarehouseRecordDto> {
    return unwrap(http.post<ApiResponse<WarehouseRecordDto>>(`/admin/warehouse/${siteId}/outbound`, { orderId, quantity }))
  },
  routes(limit = 20): Promise<RouteSnapshot> {
    return unwrap(http.get<ApiResponse<RouteSnapshot>>('/admin/routes', { params: { limit } }))
  },
  adminProfile(): Promise<{ id: number; name: string; role: string; phoneMasked: string; createdAt: string | null }> {
    return unwrap(http.get<ApiResponse<{ id: number; name: string; role: string; phoneMasked: string; createdAt: string | null }>>('/admin/profile'))
  },
  updateAdminProfile(name: string): Promise<{ id: number; name: string }> {
    return unwrap(http.patch<ApiResponse<{ id: number; name: string }>>('/admin/profile', { name }))
  },
  changeAdminPassword(oldPassword: string, newPassword: string): Promise<{ changed: boolean }> {
    return unwrap(http.post<ApiResponse<{ changed: boolean }>>('/admin/password', { oldPassword, newPassword }))
  },
}
