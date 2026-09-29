export interface ApiResponse<T> {
  code: number
  data: T
  message: string | null
}

export type Role = 'USER' | 'STAFF' | 'ADMIN'

export interface LoginResponse {
  token: string
  userId: number
  role: Role
}

export interface Demand {
  id: number
  userId: number
  title: string
  weightG: number
  volumeCm3: number
  fragile: boolean
  originRegion: string
  originAddr: string
  targetRegion: string
  targetAddr: string
  status: 'PENDING' | 'QUOTED' | 'ACCEPTED' | 'CLOSED'
  quotedPrice: number
  quotedBy: number | null
  quotedAt: string | null
  createdAt: string
}

export type OrderStatus =
  | 'PENDING' | 'PAID' | 'PICKED' | 'IN_TRANSIT' | 'ARRIVED'
  | 'DELIVERED' | 'CANCELLED' | 'REFUNDING' | 'REFUNDED'

export interface Order {
  id: number
  demandId: number
  staffId: number
  userId: number
  amount: number
  status: OrderStatus
  version: number
  paidAt: string | null
  deliveredAt: string | null
  createdAt: string
  fromLon: number | null
  fromLat: number | null
  toLon: number | null
  toLat: number | null
}

export interface Payment {
  id: number
  orderId: number
  channel: string
  txnNo: string
  amount: number
  status: string
  paidAt: string | null
  createdAt: string
}

export interface RouteTask {
  id: number
  orderId: number
  strategy: 'SHORTEST' | 'FASTEST' | 'CHEAPEST'
  distanceM: number
  etaS: number
  cost: number
  geometry: {type:string; coordinates:number[][]} | null
}

export interface LogisticsEvent {
  id: number
  orderId: number
  type: 'PICKED' | 'IN_TRANSIT' | 'ARRIVED_DELIVERY' | 'DELIVERED'
  point: string | { type: string; coordinates: number[] } | null
  occurredAt: string
  operatorId: number | null
}

export interface Notice {
  id: number
  title: string
  body: string
  status: number
  pinned: boolean
  createdAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
export interface Review {
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

export type RouteStrategy = 'SHORTEST' | 'FASTEST' | 'CHEAPEST'
