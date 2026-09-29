<template>
  <div class="od-wrap">
    <!-- 顶部状态条 -->
    <div class="od-topbar">
      <div class="od-topbar__left">
        <button class="od-back" @click="goBack">← 返回</button>
        <span class="od-order-id mono">订单 #{{ route.params.id }}</span>
      </div>
      <div class="od-topbar__right">
        <span class="od-sse">
          <span class="status-dot" :class="sseConnected ? 'status-dot--live' : 'status-dot--stale'"></span>
          <span class="mono od-sse__label">{{ sseConnected ? '实时连接' : '重连中' }}</span>
        </span>
      </div>
    </div>

    <!-- 主区：地图 + 侧栏 -->
    <div class="od-grid">
      <section class="od-map glass">
        <GisMap
          v-if="events.length > 0 || routeLines.length > 0"
          :lines="routeLines"
          :points="points"
          class="od-map__canvas"
        />
        <div v-else class="od-map__empty">
          <span class="status-dot status-dot--live"></span>
          暂无物流轨迹数据
        </div>
        <div v-if="routeInfo" class="od-map__metrics">
          <div class="od-metric">
            <span class="od-metric__label">距离</span>
            <span class="od-metric__val mono">{{ fmtDist(routeInfo.distanceM) }}</span>
          </div>
          <div class="od-metric">
            <span class="od-metric__label">预计</span>
            <span class="od-metric__val mono">{{ fmtEta(routeInfo.etaS) }}</span>
          </div>
          <div class="od-metric">
            <span class="od-metric__label">策略</span>
            <span class="od-metric__val mono">{{ strategyLabel(routeInfo.strategy) }}</span>
          </div>
        </div>
      </section>

      <aside class="od-side">
        <!-- 订单卡 -->
        <div class="od-order glass" v-if="order">
          <div class="od-order__row">
            <span class="od-order__label">金额</span>
            <span class="od-order__val mono">¥{{ fmtMoney(order.amount) }}</span>
          </div>
          <div class="od-order__row">
            <span class="od-order__label">状态</span>
            <el-tag size="small" :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
          </div>

          <div class="od-pay" v-if="order.status === 'PENDING' && !payDone">
            <el-button
              type="primary"
              class="od-pay__btn"
              :loading="paying"
              @click="onPay"
            >
              立即支付
            </el-button>
          </div>
          <div class="od-paid" v-else-if="paidTxn || payDone">
            <span class="status-dot status-dot--live"></span>
            已支付 <span class="mono">{{ paidTxn || (order.paidAt ? order.paidAt : 'PAID') }}</span>
          </div>
        </div>

        <!-- 时间线 -->
        <div class="od-timeline glass" v-if="events.length">
          <div class="od-timeline__title">轨迹时间线</div>
          <ol class="od-orbit">
            <li
              v-for="(ev, i) in events"
              :key="ev.id"
              class="od-orbit__item"
              :class="{ current: i === events.length - 1 }"
            >
              <span class="od-orbit__node"></span>
              <div class="od-orbit__content">
                <div class="od-orbit__head">
                  <span class="od-orbit__type mono">{{ eventLabel(ev.type) }}</span>
                  <span class="od-orbit__time mono">{{ ev.occurredAt?.replace('T', ' ').slice(0, 19) }}</span>
                </div>
                <span class="od-orbit__wkt mono" v-if="ev.point">{{ ev.point }}</span>
              </div>
            </li>
          </ol>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useTrackStream } from '@/composables/useTrackStream'
import { useRoute, useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import type { LogisticsEvent, RouteTask, Order, OrderStatus } from '@/types'
import GisMap from '@/components/gis/GisMap.vue'

const route = useRoute()
const router = useRouter()

function goBack() {
  if (window.history.state && window.history.state.back) router.back()
  else router.push('/user/orders')
}
const events = ref<LogisticsEvent[]>([])
const currentTask = ref<RouteTask | null>(null)
const order = ref<Order | null>(null)
const paying = ref(false)
const paidTxn = ref<string | null>(null)
const payDone = ref(false)
const sseOrderId = ref<number | null>(Number(route.params.id))
const { connected: sseConnected } = useTrackStream({
  orderId: sseOrderId,
  role: 'user',
  onSnapshot: (snap) => {
    events.value = snap
  },
  onEvent: () => {},
})

const routeInfo = computed(() => currentTask.value)

const routeLines = computed(() => {
  const geo = currentTask.value?.geometry
  if (!geo || geo.type !== 'LineString') return []
  return [{ type: 'LineString' as const, coordinates: geo.coordinates as [number, number][] }]
})

const points = computed(() => {
  const result: { lon: number; lat: number; label?: string; current?: boolean }[] = []
  for (const e of events.value) {
    const p = parsePoint(e.point, e.type)
    if (p) result.push({ lon: p.lon, lat: p.lat, label: p.label })
  }
  const last = result.length - 1
  return result.map((p, i) => ({ ...p, current: i === last }))
})
function parsePoint(point: unknown, label?: string) {
  if (!point) return null
  if (typeof point === 'string') {
    const m = point.match(/\(([-\d.]+)\s+([-\d.]+)\)/)
    if (!m) return null
    return { lon: Number(m[1]), lat: Number(m[2]), label }
  }
  const g = point as { type?: string; coordinates?: number[] }
  if (g.type === 'Point' && Array.isArray(g.coordinates) && g.coordinates.length >= 2) {
    return { lon: Number(g.coordinates[0]), lat: Number(g.coordinates[1]), label }
  }
  return null
}



function fmtMoney(fen: number) {
  return (fen / 100).toFixed(2)
}

function fmtDist(m: number) {
  return m >= 1000 ? `${(m / 1000).toFixed(1)} km` : `${m} m`
}


function fmtEta(s: number) {
  const mm = Math.floor(s / 60)
  const ss = s % 60
  if (mm >= 60) {
    const h = Math.floor(mm / 60)
    return h + " 时 " + (mm % 60) + " 分"
  }
  return mm > 0 ? mm + " 分 " + ss + " 秒" : ss + " 秒"
}
function strategyLabel(st: string): string {
  const m: Record<string, string> = { SHORTEST: '最短距离', FASTEST: '最快时效', CHEAPEST: '最低成本' }
  return m[st] ?? st
}

function eventLabel(t: string) {
  const map: Record<string, string> = {
    PICKED: '快递员取件', IN_TRANSIT: '配送在途', ARRIVED_DELIVERY: '到达网点',
    ARRIVED: '到达网点', DELIVERED: '已签收', PENDING: '订单创建', PAID: '已支付',
  }
  return map[t] ?? t
}

function statusLabel(s: OrderStatus) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '配送中',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
  }
  return map[s] ?? s
}

function statusType(s: OrderStatus) {
  if (s === 'PAID' || s === 'DELIVERED') return 'success'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'danger'
  if (s === 'IN_TRANSIT' || s === 'PICKED') return 'warning'
  return ''
}

async function onPay() {
  if (!order.value) return
  paying.value = true
  try {
    const p = await userApi.pay(order.value.id)
    await userApi.payCallback(order.value.id, p.txnNo, order.value.amount)
    paidTxn.value = p.txnNo
    payDone.value = true
    ElMessage.success('支付成功：' + p.txnNo)
    order.value = { ...order.value, status: 'PAID', paidAt: new Date().toISOString() }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '支付失败')
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  const orderId = Number(route.params.id)
  try {
    events.value = await userApi.orderLogistics(orderId)
    const orders = await userApi.myOrders(undefined, 0, 100)
    order.value = orders.content.find((o) => o.id === orderId) ?? null
    if (order.value?.status === 'PAID') {
      payDone.value = true
      paidTxn.value = order.value.paidAt || 'PAID'
    } else if (order.value?.paidAt) {
      payDone.value = true
      paidTxn.value = 'PAID'
    }
    try {
      currentTask.value = await userApi.latestRoute(orderId)
    } catch {
      /* 无路线时忽略 */
    }
  } catch (e) {
    console.error('load order detail failed', e)
  }
})
</script>

<style scoped>
.od-wrap {
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 顶部状态条 */
.od-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--glass-bg-2);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  backdrop-filter: var(--glass-blur);
}

.od-topbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.od-back {
  background: transparent;
  border: 1px solid var(--glass-border);
  color: var(--text-mid);
  border-radius: var(--radius-sm);
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s var(--ease), border-color 0.18s var(--ease);
}

.od-back:hover {
  color: var(--text-hi);
  border-color: var(--glass-border-2);
}

.od-order-id {
  font-size: 15px;
  font-weight: 700;
  color: #1e3a5f;
}

.od-sse {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.od-sse__label {
  color: var(--text-mid);
  letter-spacing: 0.08em;
}

/* 主网格 */
.od-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
}

@media (max-width: 1080px) {
  .od-grid {
    grid-template-columns: 1fr;
  }
}

/* 地图 */
.od-map {
  position: relative;
  overflow: hidden;
  min-height: 480px;
}

.od-map__canvas {
  width: 100%;
  height: 100%;
  min-height: 480px;
}

.od-map__empty {
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--text-mid);
}

.od-map__metrics {
  position: absolute;
  left: 16px;
  bottom: 16px;
  display: flex;
  gap: 18px;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(30, 58, 95, 0.18);
  border-left: 3px solid #d98e2b;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(30, 58, 95, 0.14);
}

.od-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 64px;
}

.od-metric__label {
  font-size: 10px;
  color: var(--text-low);
  letter-spacing: 0.1em;
}

.od-metric__val {
  font-size: 16px;
  font-weight: 700;
  color: #1e3a5f;
  font-variant-numeric: tabular-nums;
}

/* 侧栏 */
.od-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.od-order {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.od-order__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.od-order__label {
  font-size: 12px;
  color: var(--text-mid);
}

.od-order__val {
  font-size: 24px;
  font-weight: 700;
}

.od-pay {
  margin-top: 6px;
}

.od-pay__btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: #1e3a5f;
  border: 1px solid #16304f;
  color: #fff;
  box-shadow: 0 8px 22px rgba(30, 58, 95, 0.3);
}
.od-pay__btn:hover,
.od-pay__btn:focus {
  background: #16304f;
  border-color: #102540;
  color: #fff;
}

.od-paid {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-mid);
}

.od-paid .mono {
  color: #1e3a5f;
}

/* 时间线 */
.od-timeline {
  padding: 18px;
}

.od-timeline__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 14px;
}

.od-orbit {
  list-style: none;
  margin: 0;
  padding: 0;
  position: relative;
}

.od-orbit::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: linear-gradient(180deg, var(--primary), var(--glass-border));
}

.od-orbit__item {
  position: relative;
  padding: 0 0 18px 22px;
}

.od-orbit__item:last-child {
  padding-bottom: 0;
}

.od-orbit__node {
  position: absolute;
  left: 0;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--bg-l2);
  border: 2px solid var(--primary);
  z-index: 1;
}

.od-orbit__item.current .od-orbit__node {
  background: var(--success);
  border-color: var(--success);
  box-shadow: 0 0 0 4px rgba(22, 163, 74, 0.2);
}
.od-orbit__item.current .od-orbit__type {
  color: #1e3a5f;
}

.od-orbit__head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.od-orbit__type {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-hi);
}

.od-orbit__item.current .od-orbit__type {
  color: var(--success);
}

.od-orbit__time {
  font-size: 11px;
  color: var(--text-low);
}

.od-orbit__wkt {
  display: block;
  margin-top: 3px;
  font-size: 10px;
  color: var(--text-low);
  word-break: break-all;
}
</style>
