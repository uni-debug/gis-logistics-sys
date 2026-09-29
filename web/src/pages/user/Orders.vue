<template>
  <div class="orders-wrap">
    <div class="orders-head">
      <div class="orders-head__title">
        <h1 class="orders-title">订单中心</h1>
        <span class="orders-sub">{{ orders.length }} 条记录 · 全程可追踪</span>
      </div>
      <div class="orders-actions">
        <div class="orders-chips">
          <button
            v-for="c in statusChips"
            :key="c.value"
            class="orders-chip"
            :class="{ 'orders-chip--active': filterStatus === c.value }"
            @click="filterStatus = filterStatus === c.value ? '' : c.value"
          >{{ c.label }}</button>
        </div>
        <el-button type="primary" @click="drawerVisible = true">发起配送需求</el-button>
      </div>
    </div>

    <!-- 需求单：等待报价 / 已报价可确认成单 -->
    <section class="demand-band" v-if="demands.length">
      <div class="demand-band__head">
        <span class="demand-band__title">待处理需求单</span>
      </div>
      <div class="demand-band__list">
        <div class="demand-row" v-for="d in demands" :key="d.id">
          <div class="demand-row__top">
            <span class="demand-row__id mono">#D{{ d.id }}</span>
            <el-tag size="small" :type="demandTagType(d.status)">{{ demandLabel(d.status) }}</el-tag>
          </div>
          <div class="demand-row__body">
            <div class="demand-row__route mono">{{ d.originRegion }} → {{ d.targetRegion }}</div>
            <div class="demand-row__meta">
              <span>{{ d.title }}</span>
              <span class="mono">{{ (d.weightG / 1000).toFixed(1) }}kg</span>
            </div>
            <div class="demand-row__price" v-if="d.status === 'QUOTED'">
              <span class="demand-row__price-label">报价</span>
              <span class="demand-row__price-val mono">¥{{ fmtMoney(d.quotedPrice) }}</span>
              <el-button
                type="primary"
                size="small"
                :loading="confirming === d.id"
                @click="confirmDemand(d)"
              >确认成单</el-button>
            </div>
            <div class="demand-row__price" v-else-if="d.status === 'PENDING'">
              <span class="demand-row__hint">等待员工报价…</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div class="orders-grid">
      <div class="orders-row" v-for="o in filtered" :key="o.id">
        <div class="orders-row__top">
          <span class="orders-row__id mono">#{{ o.id }}</span>
          <div class="orders-row__statusline">
            <el-tag size="small" :type="statusType(o.status)">{{ statusLabel(o.status) }}</el-tag>
            <span class="orders-row__delivery" :class="'is-' + o.status.toLowerCase()">{{ deliveryLabel(o) }}</span>
          </div>
        </div>
        <div class="orders-row__body">
          <div class="orders-row__metric">
            <span class="orders-row__label">金额</span>
            <span class="orders-row__value mono">¥{{ fmtMoney(o.amount) }}</span>
          </div>
          <div class="orders-row__coords mono" v-if="o.fromLon">
            [{{ o.fromLon?.toFixed(3) }}, {{ o.fromLat?.toFixed(3) }}]
          </div>
          <div class="orders-row__thumb" v-if="trackPoints[o.id]?.length">
            <GisMap :points="trackPoints[o.id]" :zoom="4" class="orders-row__thumb-map" />
          </div>
        </div>
        <div class="orders-row__foot">
          <span class="orders-row__date mono">{{ o.createdAt?.slice(0, 10) }}</span>
          <el-button link type="primary" @click="router.push(`/user/orders/${o.id}`)">查看轨迹 →</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!demands.length && !filtered.length" description="暂无需求单与订单" />

    <!-- 发起需求抽屉 -->
    <el-drawer v-model="drawerVisible" title="发起配送需求" size="380px">
      <el-form :model="form" label-position="top" class="demand-form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="如：济南 → 北京 一件快递" />
        </el-form-item>
        <el-form-item label="重量(g)">
          <el-input-number v-model="form.weightG" :min="1" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体积(cm³)">
          <el-input-number v-model="form.volumeCm3" :min="1" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="易碎品">
          <el-switch v-model="form.fragile" />
        </el-form-item>
        <el-form-item label="出发区域">
          <el-select v-model="form.originRegion" placeholder="选择省份">
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="出发地址">
          <el-select v-model="form.originCity" placeholder="选择城市" style="width: 55%; margin-right: 6px">
            <el-option v-for="c in originCities" :key="c" :label="c" :value="c" />
          </el-select>
          <el-input v-model="form.originAddr" placeholder="区县 / 街道 / 门牌" style="width: 40%" />
        </el-form-item>
        <el-form-item label="目的区域">
          <el-select v-model="form.targetRegion" placeholder="选择省份">
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="目的地址">
          <el-select v-model="form.targetCity" placeholder="选择城市" style="width: 55%; margin-right: 6px">
            <el-option v-for="c in targetCities" :key="c" :label="c" :value="c" />
          </el-select>
          <el-input v-model="form.targetAddr" placeholder="区县 / 街道 / 门牌" style="width: 40%" />
        </el-form-item>
        <el-button
          type="primary"
          style="width: 100%"
          :loading="publishing"
          @click="publish"
        >发布需求</el-button>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import GisMap from '@/components/gis/GisMap.vue'
import chinaRegions from '@/data/chinaRegions.json'
import type { Order, OrderStatus, Demand, LogisticsEvent } from '@/types'

const router = useRouter()
const orders = ref<Order[]>([])
const demands = ref<Demand[]>([])
const filterStatus = ref('')
const statusChips = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING' },
  { label: '配送中', value: 'IN_TRANSIT' },
  { label: '已签收', value: 'DELIVERED' },
]


const trackPoints = ref<Record<number, { lon: number; lat: number; current?: boolean; label?: string }[]>>({})

const filtered = computed(() => {
  const kw = filterStatus.value.trim().toUpperCase()
  if (!kw) return orders.value
  // chip 值精确匹配，否则按关键字子串
  if (['PENDING','PAID','PICKED','IN_TRANSIT','ARRIVED','DELIVERED','CANCELLED','REFUNDING','REFUNDED'].includes(kw)) {
    return orders.value.filter((o) => o.status === kw)
  }
  return orders.value.filter((o) => o.status.includes(kw) || String(o.id) === kw)
})

interface Region { name: string; cities: { name: string; districts: string[] }[] }
const regionData = chinaRegions as unknown as { provinces: Region[] }
const provinces = regionData.provinces.map((p) => p.name)
function citiesOf(prov: string): string[] {
  const p = regionData.provinces.find((x) => x.name === prov)
  return p ? p.cities.map((c) => c.name) : []
}

const form = reactive({
  title: '', weightG: 1000, volumeCm3: 500, fragile: false,
  originRegion: '', originCity: '', originAddr: '',
  targetRegion: '', targetCity: '', targetAddr: '',
})
const originCities = computed(() => citiesOf(form.originRegion))
const targetCities = computed(() => citiesOf(form.targetRegion))
const drawerVisible = ref(false)
const publishing = ref(false)
const confirming = ref<number | null>(null)

async function publish() {
  if (!form.title || !form.originRegion || !form.targetRegion) {
    ElMessage.warning('请填写标题与出发/目的区域')
    return
  }
  publishing.value = true
  try {
    await userApi.publishDemand({
      title: form.title,
      weightG: form.weightG,
      volumeCm3: form.volumeCm3,
      fragile: form.fragile,
      originRegion: form.originRegion,
      originAddr: [form.originCity, form.originAddr].filter(Boolean).join(' '),
      targetRegion: form.targetRegion,
      targetAddr: [form.targetCity, form.targetAddr].filter(Boolean).join(' '),
    })
    ElMessage.success('需求已发布，等待员工报价')
    drawerVisible.value = false
    load()
  } catch (e) {
    console.error('publish demand failed', e)
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

async function confirmDemand(d: Demand) {
  confirming.value = d.id
  try {
    await userApi.confirmDemand(d.id)
    ElMessage.success('已确认成单')
    load()
  } catch (e) {
    console.error('confirm demand failed', e)
    ElMessage.error('确认失败')
  } finally {
    confirming.value = null
  }
}

function statusType(s: OrderStatus) {
  if (s === 'DELIVERED') return 'success'
  if (s === 'CANCELLED' || s === 'REFUNDED') return 'danger'
  if (s === 'IN_TRANSIT' || s === 'PICKED') return 'warning'
  return 'info'
}

function demandTagType(s: Demand['status']) {
  if (s === 'QUOTED') return 'success'
  if (s === 'PENDING') return 'warning'
  return 'info'
}

function demandLabel(s: Demand['status']) {
  const map: Record<Demand['status'], string> = {
    PENDING: '待报价', QUOTED: '已报价', ACCEPTED: '已成单', CLOSED: '已关闭',
  }
  return map[s] ?? s
}

function parsePoint(point: unknown): { lon: number; lat: number } | null {
  if (!point) return null
  if (typeof point === 'string') {
    const m = point.match(/\(([-\d.]+)\s+([-=\d.]+)\)/)
    if (!m) return null
    return { lon: Number(m[1]), lat: Number(m[2]) }
  }
  const g = point as { type?: string; coordinates?: number[] }
  if (g.type === 'Point' && Array.isArray(g.coordinates) && g.coordinates.length >= 2) {
    return { lon: Number(g.coordinates[0]), lat: Number(g.coordinates[1]) }
  }
  return null
}

function fmtMoney(fen: number) {
  return (fen / 100).toFixed(2)
}

async function loadTracks() {
  const targets = filtered.value.filter((o) => o.fromLon != null)
  for (const o of targets.slice(0, 8)) {
    try {
      const evs = await userApi.orderLogistics(o.id)
      const pts: { lon: number; lat: number; current?: boolean; label?: string }[] = []
      evs.forEach((e: LogisticsEvent) => {
        const p = parsePoint(e.point)
        if (p) pts.push({ ...p, label: e.type, current: false })
      })
      if (o.fromLon != null && o.fromLat != null) pts.unshift({ lon: o.fromLon, lat: o.fromLat, label: 'FROM', current: false })
      if (o.toLon != null && o.toLat != null) pts.push({ lon: o.toLon, lat: o.toLat, label: 'TO', current: true })
      trackPoints.value[o.id] = pts
    } catch { /* ignore */ }
  }
}

function statusLabel(s: OrderStatus) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '在途',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
  }
  return map[s] ?? s
}
function deliveryLabel(o: Order) {
  if (o.status === 'PENDING') return '等待支付'
  if (o.status === 'PAID') return '已支付 · 待揽收'
  if (o.status === 'PICKED') return '快递员已取件'
  if (o.status === 'IN_TRANSIT') return '配送中'
  if (o.status === 'ARRIVED') return '已到达网点'
  if (o.status === 'DELIVERED') return '已签收'
  if (o.status === 'CANCELLED') return '已取消'
  return '—'
}

async function load() {
  try {
    const [o, d] = await Promise.all([userApi.myOrders(), userApi.myDemands()])
    orders.value = o.content
    demands.value = d.content.filter((x) => x.status === 'PENDING' || x.status === 'QUOTED')
    loadTracks()
  } catch (e) {
    console.error('load orders failed', e)
  }
}

const route = useRoute()
onMounted(() => {
  load()
  if (route.query.new === '1') drawerVisible.value = true
})
</script>

<style scoped>
.orders-wrap {
  min-height: calc(100vh - 108px);
}

.orders-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 22px;
  gap: 16px;
  flex-wrap: wrap;
}

.orders-head__title { position: relative; padding-left: 14px; }
.orders-head__title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 2px;
  bottom: 2px;
  width: 4px;
  border-radius: 2px;
  background: #d98e2b;
  box-shadow: 0 0 0 3px rgba(217, 142, 43, 0.16);
}

.orders-title {
  margin: 0 0 4px;
  font-size: 26px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: 0.01em;
}

.orders-sub {
  font-size: 12px;
  color: var(--text-low);
}

.orders-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.orders-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.orders-chip {
  padding: 6px 12px;
  font-size: 12px;
  color: var(--text-mid);
  background: transparent;
  border: 1px solid var(--glass-border);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.16s var(--ease);
}
.orders-chip:hover { color: var(--text-hi); border-color: var(--glass-border-2); }
.orders-chip--active {
  color: var(--text-hi);
  background: rgba(150, 158, 172, 0.16);
  border-color: var(--primary);
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.orders-row {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(30, 58, 95, 0.05);
  transition: transform 0.18s var(--ease), box-shadow 0.18s var(--ease), border-color 0.18s var(--ease);
  position: relative;
  overflow: hidden;
}
.orders-row::before {
  content: "";
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: var(--glass-border-2);
  transition: background 0.18s var(--ease);
}
.orders-row:hover {
  transform: translateY(-2px);
  border-color: rgba(30, 58, 95, 0.3);
  box-shadow: 0 10px 26px rgba(30, 58, 95, 0.12);
}
.orders-row:hover::before { background: #d98e2b; }

.orders-row__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.orders-row__id {
  font-size: 20px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: -0.01em;
}

.orders-row__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.orders-row__label {
  font-size: 11px;
  color: var(--text-mid);
}

.orders-row__value {
  font-size: 24px;
  font-weight: 600;
}

.orders-row__coords {
  font-size: 11px;
  color: var(--text-low);
}

.orders-row__statusline {
  display: flex;
  align-items: center;
  gap: 8px;
}
.orders-row__delivery {
  font-size: 11px;
  letter-spacing: 0.04em;
  color: var(--text-mid);
}
.orders-row__delivery.is-in_transit { color: var(--accent-bright); }
.orders-row__delivery.is-delivered,
.orders-row__delivery.is-arrived { color: var(--success); }
.orders-row__delivery.is-pending { color: var(--text-low); }
.orders-row__delivery.is-paid,
.orders-row__delivery.is-picked { color: var(--primary); }
.orders-row__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--glass-border);
  padding-top: 12px;
}

.orders-row__date {
  font-size: 12px;
  color: var(--text-mid);
}

.demand-band {
  margin-bottom: 22px;
}
.orders-grid .orders-row,
.demand-band {
  animation: orders-in 0.4s var(--ease) both;
}
.orders-grid .orders-row:nth-child(2) { animation-delay: 0.05s; }
.orders-grid .orders-row:nth-child(3) { animation-delay: 0.1s; }
.orders-grid .orders-row:nth-child(4) { animation-delay: 0.15s; }
.orders-grid .orders-row:nth-child(5) { animation-delay: 0.2s; }
.orders-grid .orders-row:nth-child(6) { animation-delay: 0.25s; }
@keyframes orders-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .orders-grid .orders-row, .demand-band { animation: none; }
}

.demand-band__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.demand-band__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-mid);
}

.demand-band__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.demand-row {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(217, 142, 43, 0.05);
  border: 1px solid rgba(217, 142, 43, 0.22);
  border-radius: 12px;
}

.demand-row__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.demand-row__id {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary);
}

.demand-row__route {
  font-size: 12px;
  color: var(--text-mid);
}

.demand-row__meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.demand-row__price {
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid var(--glass-border);
  padding-top: 10px;
}

.demand-row__price-label {
  font-size: 11px;
  color: var(--text-low);
}

.demand-row__price-val {
  font-size: 18px;
  font-weight: 600;
}

.demand-row__hint {
  font-size: 12px;
  color: var(--text-low);
}

.demand-form {
  padding: 4px 0;
}

.orders-row__thumb {
  height: 150px;
  margin-top: 4px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--glass-border);
  background: #0f172a;
}

.orders-row__thumb-map .gis-map {
  height: 150px !important;
  border-radius: 8px;
}
</style>