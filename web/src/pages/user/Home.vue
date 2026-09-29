<template>
  <div class="home-wrap">
    <!-- 左侧：状态面板 -->
    <aside class="home-side">
      <section class="home-hero glass">
        <div class="home-hero__label mono">
          <span class="home-hero__live"><span class="status-dot status-dot--live"></span>实时</span>
          用户 #{{ auth.userId }}
        </div>
        <h1 class="home-hero__title">{{ greeting() }}，你的包裹{{ inTransitCount > 0 ? '正在路上' : '状态一览' }}</h1>
        <p class="home-hero__desc">实时轨迹、订单动态、评价与反馈，一站掌握</p>
        <div class="home-hero__cta">
          <div class="home-hero__cta-primary">
            <el-button class="cta-btn cta-btn--primary" @click="router.push('/user/orders')">
              进入订单中心
            </el-button>
            <el-button class="cta-btn cta-btn--ghost" @click="router.push('/user/profile')">
              个人中心
            </el-button>
          </div>
          <div class="home-hero__cta-secondary">
            <el-button class="cta-chip" @click="router.push('/user/orders?new=1')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
              发起配送
            </el-button>
            <el-button class="cta-chip" @click="router.push('/user/notices')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 9a7 7 0 0 1 14 0c0 5 2 6.5 2 6.5H3S5 14 5 9zm4.5 9a2.5 2.5 0 0 0 5 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
              系统公告
            </el-button>
            <el-button class="cta-chip cta-chip--danger" @click="logout">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2m4-6h4m0 0l-3-3m3 3l-3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
              退出
            </el-button>
          </div>
        </div>
      </section>

      <section class="home-stats">
        <div class="kpi-row">
          <div class="kpi kpi--hero">
            <div class="kpi__tag">总量</div>
            <div class="kpi__num mono">{{ orders.length }}</div>
            <div class="kpi__label">订单总数</div>
          </div>
          <div class="kpi">
            <div class="kpi__tag">在途</div>
            <div class="kpi__num mono kpi__num--live">{{ inTransitCount }}</div>
            <div class="kpi__label">在途订单</div>
          </div>
          <div class="kpi">
            <div class="kpi__tag">签收</div>
            <div class="kpi__num mono">{{ deliveredCount }}</div>
            <div class="kpi__label">已签收</div>
          </div>
          <div class="kpi kpi--money">
            <div class="kpi__tag">金额</div>
            <div class="kpi__num mono kpi__num--money">¥{{ fmtMoney(totalFen) }}</div>
            <div class="kpi__label">成交总额</div>
          </div>
        </div>
      </section>

      <section class="home-recent" v-if="orders.length">
        <div class="home-recent__head">
          <span class="home-recent__title">最近订单<span class="home-recent__hint mono">最近 4 条</span></span>
          <el-link :underline="false" @click="router.push('/user/orders')">全部 →</el-link>
        </div>
        <ul class="home-recent__list">
          <li
            v-for="o in orders.slice(0, 4)"
            :key="o.id"
            class="home-recent__item"
            @click="router.push(`/user/orders/${o.id}`)"
          >
            <div class="home-recent__info">
              <div class="home-recent__id mono">#{{ o.id }}</div>
              <div class="home-recent__item-label">{{ itemLabel(o) }}</div>
            </div>
            <div class="home-recent__right">
              <el-tag size="small" :type="statusType(o.status)">{{ statusLabel(o.status) }}</el-tag>
              <span class="home-recent__chev">›</span>
            </div>
          </li>
        </ul>
      </section>
    </aside>

    <!-- 右侧：主地图区 -->
    <main class="home-map glass">
      <div class="home-map__head">
        <div class="home-map__headline">
          <span class="home-map__title">实时轨迹</span>
          <span class="home-map__sub mono" v-if="activeOrder">ORDER #{{ activeOrder.id }}</span>
        </div>
        <div class="home-map__progress" v-if="activeOrder">
          <div class="home-map__progress-top">
            <span class="home-map__progress-label">{{ progressLabel }}</span>
            <span class="home-map__progress-pct mono">{{ progressPct }}%</span>
          </div>
          <div class="home-map__progress-bar">
            <div class="home-map__progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>
        </div>
      </div>
      <GisMap
        v-if="hasMapData"
        :lines="routeLines"
        :points="points"
        class="home-map__canvas"
      />
      <div v-else class="home-map__empty">
        <div class="home-map__empty-ring"></div>
        <span class="home-map__empty-title">暂无在途轨迹</span>
        <span class="home-map__empty-sub mono">员工接单并规划路线后，这里会实时显示位置</span>
        <el-button type="primary" plain size="small" @click="router.push('/user/orders')">去订单中心</el-button>
      </div>
      <section v-if="routeInfo" class="home-routemetrics glass">
        <div class="home-routemetrics__item">
          <span class="home-routemetrics__label">路线距离</span>
          <span class="home-routemetrics__val mono">{{ fmtDist(routeInfo.distanceM) }}</span>
        </div>
        <div class="home-routemetrics__item">
          <span class="home-routemetrics__label">预计时长</span>
          <span class="home-routemetrics__val mono">{{ fmtEta(routeInfo.etaS) }}</span>
        </div>
        <div class="home-routemetrics__item">
          <span class="home-routemetrics__label">策略成本</span>
          <span class="home-routemetrics__val mono home-routemetrics__val--cost">¥{{ fmtMoney(routeInfo.cost) }}</span>
        </div>
        <div class="home-routemetrics__item">
          <span class="home-routemetrics__label">规划策略</span>
          <span class="home-routemetrics__val home-routemetrics__val--strategy">{{ strategyLabel(routeInfo.strategy) }}</span>
        </div>
      </section>

      <section v-if="activeOrder" class="home-timeline glass">
        <div class="home-timeline__head">
          <span class="home-timeline__title">配送动态</span>
          <span class="home-timeline__order mono">ORDER #{{ activeOrder.id }}</span>
        </div>
        <ol class="home-timeline__list">
          <li
            v-for="(e, i) in timeline"
            :key="i"
            class="home-timeline__item"
            :class="{ 'home-timeline__item--latest': i === timeline.length - 1 }"
          >
            <span class="home-timeline__dot" :class="dotClass(e.type)"></span>
            <div class="home-timeline__body">
              <span class="home-timeline__type">{{ eventLabel(e.type) }}</span>
              <span class="home-timeline__time mono">{{ e.occurredAt?.replace('T', ' ').slice(0, 16) }}</span>
            </div>
          </li>
        </ol>
        <div v-if="timeline.length === 0" class="home-timeline__empty">
          该订单暂无轨迹事件，员工接单规划路线后会实时出现在这里
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTrackStream } from '@/composables/useTrackStream'
import { userApi } from '@/api/user'
import GisMap from '@/components/gis/GisMap.vue'
import type { Order, Demand, LogisticsEvent, RouteTask } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const orders = ref<Order[]>([])
const activeOrder = ref<Order | null>(null)
const activeEvents = ref<LogisticsEvent[]>([])
const activeTask = ref<RouteTask | null>(null)
// 配送进度：按订单状态推进一步，叠加在途事件占比
const progressPct = computed(() => {
  if (!activeOrder.value) return 0
  const stepByStatus: Record<string, number> = {
    PENDING: 5, PAID: 15, PICKED: 40, IN_TRANSIT: 70, ARRIVED: 90, DELIVERED: 100,
  }
  const base = stepByStatus[activeOrder.value.status] ?? 0
  if (base >= 100) return 100
  const total = base + Math.min(1, activeEvents.value.length / 8) * (100 - base)
  return Math.round(Math.min(100, total))
})
const progressLabel = computed(() => {
  if (!activeOrder.value) return '未开始'
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '配送中',
    ARRIVED: '已到达', DELIVERED: '已签收',
  }
  return map[activeOrder.value.status] ?? activeOrder.value.status
})

const inTransitCount = computed(
  () => orders.value.filter((o) => ['PICKED', 'IN_TRANSIT', 'ARRIVED'].includes(o.status)).length,
)

// 默认选中第一条在途/已到达订单做实时轨迹展示
function pickActive() {
  const live = orders.value.find((o) => ['PICKED', 'IN_TRANSIT', 'ARRIVED', 'DELIVERED'].includes(o.status))
  activeOrder.value = live ?? orders.value[0] ?? null
}

onMounted(async () => {
  try {
    const [res, dr] = await Promise.all([
      userApi.myOrders(undefined, 0, 50),
      userApi.myDemands().catch(() => null),
    ])
    orders.value = res.content
    homeDemands.value = dr?.content ?? []
    pickActive()
  } catch (e) {
    console.error('load home orders failed', e)
  }
})

// 选中的订单变化时，拉它的轨迹 + 路线 + SSE 实时流
const { open, close } = useTrackStream({
  orderId: computed(() => activeOrder.value?.id ?? null),
  role: 'user',
  onSnapshot: (snap) => { activeEvents.value = snap },
})
watch(activeOrder, async (o) => {
  activeEvents.value = []
  activeTask.value = null
  close()
  if (!o) return
  try {
    activeEvents.value = await userApi.orderLogistics(o.id)
    activeTask.value = await userApi.latestRoute(o.id).catch(() => null)
    open()
  } catch {
    /* ignore */
  }
}, { immediate: true })


const totalFen = computed(() => orders.value.reduce((s, o) => s + o.amount, 0))
const deliveredCount = computed(() => orders.value.filter((o) => o.status === 'DELIVERED').length)

const homeDemands = ref<Demand[]>([])
const demandById = computed(() => {
  const m = new Map<number, Demand>()
  for (const d of homeDemands.value) m.set(d.id, d)
  return m
})
function itemLabel(o: Order): string {
  const d = demandById.value.get(o.demandId)
  if (!d) return "—"
  return d.originRegion + " → " + d.targetRegion + " · " + d.title
}
function greeting(): string {
  const h = new Date().getHours()
  if (h >= 5 && h < 11) return "早上好"
  if (h >= 11 && h < 14) return "中午好"
  if (h >= 14 && h < 18) return "下午好"
  return "晚上好"
}


const timeline = computed(() => [...activeEvents.value].reverse())

function dotClass(t: string): string {
  const m: Record<string, string> = {
    PICKED: 'dot--picked', IN_TRANSIT: 'dot--transit', ARRIVED_DELIVERY: 'dot--arrived', DELIVERED: 'dot--done',
  }
  return m[t] ?? 'dot--default'
}
function eventLabel(t: string): string {
  const m: Record<string, string> = {
    PICKED: '已揽件', IN_TRANSIT: '运输中', ARRIVED_DELIVERY: '到达目的地', DELIVERED: '已签收',
  }
  return m[t] ?? t
}

const routeInfo = computed(() => activeTask.value)

function fmtDist(m: number): string {
  if (m >= 10000) return (m / 1000).toFixed(1) + ' km'
  if (m >= 1000) return (m / 1000).toFixed(2) + ' km'
  return m + ' m'
}
function fmtEta(s: number): string {
  if (s < 60) return s + ' 秒'
  if (s < 3600) return Math.round(s / 60) + ' 分钟'
  const h = Math.floor(s / 3600)
  const mm = Math.round((s % 3600) / 60)
  return h + ' 时 ' + mm + ' 分'
}
function strategyLabel(st: string): string {
  const m: Record<string, string> = { SHORTEST: '最短路', FASTEST: '最快路线', CHEAPEST: '成本最优' }
  return m[st] ?? st
}

const hasMapData = computed(
  () => activeEvents.value.length > 0 || routeLines.value.length > 0,
)

const routeLines = computed(() => {
  const geo = activeTask.value?.geometry
  if (!geo || geo.type !== 'LineString') return []
  return [{ type: 'LineString' as const, coordinates: geo.coordinates as [number, number][] }]
})

const points = computed(() => {
  const result: { lon: number; lat: number; label?: string; current?: boolean }[] = []
  for (const e of activeEvents.value) {
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


function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '配送中',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
  }
  return map[s] ?? s
}

function statusType(s: string) {
  if (s === 'DELIVERED') return 'success'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'danger'
  if (s === 'IN_TRANSIT' || s === 'PICKED') return 'warning'
  return 'info'
}
function fmtMoney(fen: number) {
  return (fen / 100).toFixed(2)
}
function logout() {
  auth.logout()
  router.push('/user/login')
}
</script>

<style scoped>
.home-wrap {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  min-height: calc(100vh - 108px);
}
.home-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.home-side > *,
.home-map {
  animation: home-in 0.45s var(--ease) both;
}
.home-map { animation-delay: 0.08s; }
@keyframes home-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .home-side > *, .home-map { animation: none; }
}
.home-hero {
  padding: 20px;
}
.home-stats {
  margin-top: 0;
}
.home-hero__label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--text-low);
  margin-bottom: 10px;
}
.home-hero__live {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--success);
}
.home-hero__title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
}
.home-hero__desc {
  margin: 0 0 16px;
  font-size: 12.5px;
  color: var(--text-mid);
  line-height: 1.6;
}
.home-hero__cta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 4px;
}
.home-hero__cta-primary {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 10px;
}
.cta-btn {
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  border-radius: var(--radius-md);
}
.home-hero__cta-primary {
  grid-template-columns: 1.4fr 1fr;
}
.cta-btn--primary {
  width: 100%;
  border: 1px solid var(--primary);
  background: #35465b;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(53, 70, 91, 0.22);
}
.cta-btn--primary:hover {
  background: #2c3b4d;
  border-color: #2c3b4d;
  color: #ffffff;
}
.cta-btn--ghost {
  width: 100%;
  border: 1px solid var(--glass-border-2);
  background: none;
  color: var(--text-mid);
}
.cta-btn--ghost:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(79, 98, 117, 0.05);
}
.home-hero__cta-secondary {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.cta-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  font-size: 12.5px;
  font-weight: 500;
  border: 1px solid var(--glass-border);
  background: var(--glass-bg-2);
  color: var(--text-mid);
  border-radius: var(--radius-md);
}
.cta-chip svg { flex: 0 0 14px; opacity: 0.85; }
.cta-chip:not(.cta-chip--danger):hover { color: #1e3a5f; border-color: rgba(30, 58, 95, 0.4); }
.cta-chip:not(.cta-chip--danger):hover svg { opacity: 1; }
.cta-chip:hover {
  border-color: var(--glass-border-2);
  color: var(--text-hi);
  background: var(--bg-l2);
}
.cta-chip--danger {
  color: var(--destructive);
  border-color: rgba(214, 69, 69, 0.24);
}
.cta-chip--danger:hover {
  color: var(--destructive);
  border-color: var(--destructive);
  background: rgba(214, 69, 69, 0.06);
}
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  border-top: 1px solid var(--glass-border);
  padding-top: 14px;
}
.kpi {
  position: relative;
  padding: 14px 14px 12px;
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  background: var(--glass-bg-2);
  overflow: hidden;
  transition: transform 0.18s var(--ease), box-shadow 0.18s var(--ease), border-color 0.18s var(--ease);
}
.kpi:hover {
  transform: translateY(-2px);
  border-color: var(--glass-border-2);
  box-shadow: 0 8px 20px rgba(30, 58, 95, 0.1);
}
.kpi--hero {
  background: #1e3a5f;
  border-color: #16304f;
}
.kpi--hero .kpi__num { color: #fff; }
.kpi--hero .kpi__label { color: rgba(255, 255, 255, 0.65); }
.kpi--hero .kpi__tag { color: #d98e2b; background: rgba(217, 142, 43, 0.16); }
.kpi--hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(220px 90px at 85% 0%, rgba(217, 142, 43, 0.14), transparent 60%),
    repeating-linear-gradient(90deg, transparent 0 24px, rgba(255, 255, 255, 0.045) 24px 25px);
  pointer-events: none;
}
.kpi__tag {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-low);
  margin-bottom: 8px;
}
.kpi__num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-hi);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}
.kpi__num--live { color: #d97706; }
.kpi--hero .kpi__num--live { color: #ffb45e; }
.kpi__num--money { color: #1e3a5f; }
.kpi__label {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-mid);
  letter-spacing: 0.04em;
}
.home-recent__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.home-recent__title {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.home-recent__hint {
  font-size: 10px;
  color: var(--text-low);
}
.home-recent__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.home-recent__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px;
  border-bottom: 1px solid var(--glass-border);
  cursor: pointer;
  transition: background 0.18s var(--ease);
}
.home-recent__item:hover { background: rgba(79, 98, 117, 0.07); }
.home-recent__info { min-width: 0; }
.home-recent__id {
  font-size: 13px;
  color: var(--text-hi);
  font-weight: 600;
}
.home-recent__item-label {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-low);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}
.home-recent__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.home-recent__chev {
  color: var(--text-low);
  font-size: 14px;
  transition: transform 0.18s var(--ease), color 0.18s var(--ease);
}
.home-recent__item:hover .home-recent__chev {
  color: var(--primary);
  transform: translateX(3px);
}
.home-map {
  position: relative;
  min-height: 520px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.home-map__head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--glass-border);
}
.home-map__headline {
  display: flex;
  align-items: center;
  gap: 10px;
}
.home-map__progress-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-mid);
}
.home-map__progress-label { font-weight: 600; color: var(--text-hi); }
.home-map__progress-pct { color: var(--accent-bright); font-size: 12px; }
.home-map__progress-bar {
  height: 6px;
  border-radius: 999px;
  background: var(--glass-bg-2);
  overflow: hidden;
}
.home-map__progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--primary), var(--success));
  transition: width 0.6s var(--ease);
}
@media (prefers-reduced-motion: reduce) {
  .home-map__progress-fill { transition: none; }
}
.home-map__title {
  font-size: 14px;
  font-weight: 600;
}
.home-map__sub {
  font-size: 11px;
  color: var(--text-low);
}
.home-map__canvas {
  flex: 1;
  min-height: 480px;
}
.home-map__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-mid);
  font-size: 13px;
  padding: 24px;
}
.home-map__empty-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid var(--glass-border-2);
  display: grid;
  place-items: center;
  background: radial-gradient(circle at 50% 50%, rgba(79, 140, 255, 0.14), transparent 70%);
  animation: ring-pulse 2.4s var(--ease) infinite;
}
.home-map__empty-ring::after {
  content: "";
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 14px var(--primary);
}
@keyframes ring-pulse {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.08); opacity: 0.6; }
}
.home-map__empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-hi);
}
.home-map__empty-sub {
  font-size: 12px;
  color: var(--text-low);
  letter-spacing: 0.03em;
}

/* ---------- 路线指标 ---------- */
.home-routemetrics {
  margin-top: 14px;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.home-routemetrics__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  background: var(--glass-bg-2);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
}
.home-routemetrics__label {
  font-size: 11px;
  color: var(--text-low);
  letter-spacing: 0.04em;
}
.home-routemetrics__val {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-hi);
}
.home-routemetrics__val--cost { color: var(--primary); }
.home-routemetrics__val--strategy { font-size: 13px; }

/* ---------- 配送动态时间线 ---------- */
.home-timeline {
  margin-top: 14px;
  padding: 16px 18px;
}
@media (max-width: 720px) {
  .home-routemetrics { grid-template-columns: repeat(2, 1fr); }
  .home-hero__cta-primary { grid-template-columns: 1fr; }
  .home-hero__cta-secondary { grid-template-columns: 1fr 1fr; }
}
.home-timeline__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.home-timeline__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-hi);
}
.home-timeline__order {
  font-size: 11px;
  color: var(--text-low);
}
.home-timeline__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.home-timeline__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: var(--glass-bg-2);
}
.home-timeline__item--latest {
  border-color: rgba(79, 98, 117, 0.35);
  background: rgba(79, 98, 117, 0.07);
}
.home-timeline__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot--picked { background: #0891b2; box-shadow: 0 0 8px rgba(8, 145, 178, 0.5); }
.dot--transit { background: #d97706; box-shadow: 0 0 8px rgba(217, 119, 6, 0.5); }
.dot--arrived { background: #7c3aed; box-shadow: 0 0 8px rgba(124, 58, 237, 0.5); }
.dot--done { background: #16a34a; box-shadow: 0 0 8px rgba(22, 163, 74, 0.5); }
.dot--default { background: var(--text-low); }
.home-timeline__body {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}
.home-timeline__type {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-hi);
}
.home-timeline__time {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--text-low);
}
.home-timeline__empty {
  padding: 18px;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-low);
}
@media (prefers-reduced-motion: reduce) {
  .home-map__empty-ring { animation: none; }
}
@media (max-width: 900px) {
  .home-wrap { grid-template-columns: 1fr; }
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
}
@media (prefers-reduced-motion: reduce) {
  .home-recent__item { transition: none; }
}
</style>
