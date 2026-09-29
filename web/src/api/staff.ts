import { http, unwrap } from './http'
import type { ApiResponse, Demand, Page, Order, OrderStatus, RouteTask, RouteStrategy, LogisticsEvent, Review } from '@/types'

export interface PlanRouteParams {
  strategy: RouteStrategy
  fromLon?: number
  fromLat?: number
  toLon?: number
  toLat?: number
  via?: number[][]
  avoid?: string[]
}

export interface EndpointCoords {
  fromLon: number
  fromLat: number
  toLon: number
  toLat: number
}

export interface StaffProfileView {
  userId: number
  name: string | null
  phoneMask: string | null
  licenseNo: string
  siteName: string | null
  totalOrders: number
  delivered: number
  inTransit: number
  avgRating: number | null
}

export interface EventBody {
  type: LogisticsEvent['type']
  lon?: number | null
  lat?: number | null
}

export interface EventBodyWithLabel extends EventBody {
  label?: string
}

export const staffApi = {
  openDemands(page = 0, size = 20): Promise<Page<Demand>> {
    return unwrap(http.get<ApiResponse<Page<Demand>>>('/staff/demands', { params: { page, size } }))
  },
  quote(demandId: number, priceFen: number): Promise<Demand> {
    return unwrap(http.post<ApiResponse<Demand>>(`/staff/demands/${demandId}/quote`, { priceFen }))
  },
  myOrders(status?: OrderStatus, page = 0, size = 20): Promise<Page<Order>> {
    return unwrap(http.get<ApiResponse<Page<Order>>>('/staff/orders', { params: { status, page, size } }))
  },
  transition(orderId: number, status: OrderStatus): Promise<Order> {
    return unwrap(http.patch<ApiResponse<Order>>(`/staff/orders/${orderId}/status`, { status }))
  },
  planRoute(orderId: number, params: PlanRouteParams): Promise<RouteTask> {
    return unwrap(http.post<ApiResponse<RouteTask>>(`/staff/orders/${orderId}/route`, params))
  },
  latestRoute(orderId: number): Promise<RouteTask> {
    return unwrap(http.get<ApiResponse<RouteTask>>(`/staff/orders/${orderId}/route`))
  },
  orderRegions(orderId: number): Promise<{ originRegion: string; targetRegion: string; originAddr: string; targetAddr: string }> {
    return unwrap(http.get<ApiResponse<{ originRegion: string; targetRegion: string; originAddr: string; targetAddr: string }>>(`/staff/orders/${orderId}/regions`))
  },
  publishEvent(orderId: number, body: EventBody): Promise<LogisticsEvent> {
    return unwrap(http.post<ApiResponse<LogisticsEvent>>(`/staff/orders/${orderId}/events`, body))
  },
  orderEvents(orderId: number): Promise<LogisticsEvent[]> {
    return unwrap(http.get<ApiResponse<LogisticsEvent[]>>(`/staff/orders/${orderId}/events`))
  },
  setEndpoints(orderId: number, body: EndpointCoords): Promise<Order> {
    return unwrap(http.patch<ApiResponse<Order>>(`/staff/orders/${orderId}/endpoints`, body))
  },
  myReviews(): Promise<Review[]> {
    return unwrap(http.get<ApiResponse<Review[]>>('/staff/reviews'))
  },
  replyReview(reviewId: number, reply: string): Promise<Review> {
    return unwrap(http.post<ApiResponse<Review>>(`/staff/reviews/${reviewId}/reply`, { reply }))
  },
  myProfile(): Promise<StaffProfileView> {
    return unwrap(http.get<ApiResponse<StaffProfileView>>('/staff/profile'))
  },
  updateProfile(name: string): Promise<{ userId: number; name: string }> {
    return unwrap(http.patch<ApiResponse<{ userId: number; name: string }>>('/staff/profile', { name }))
  },
  changePassword(oldPassword: string, newPassword: string): Promise<{ changed: boolean }> {
    return unwrap(http.post<ApiResponse<{ changed: boolean }>>('/staff/password', { oldPassword, newPassword }))
  },
  updateLicense(licenseNo: string): Promise<{ licenseNo: string }> {
    return unwrap(http.patch<ApiResponse<{ licenseNo: string }>>('/staff/profile/license', { licenseNo }))
  },
}