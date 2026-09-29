<template>
  <div class="svc-wrap">
    <div class="svc-head">
      <div class="svc-head__title">
        <h1 class="svc-title">服务与反馈</h1>
        <span class="svc-sub">订单评价、留言反馈与在线客服</span>
      </div>
    </div>

    <div class="svc-grid">
      <!-- 订单列表 -->
      <section class="svc-panel glass">
        <div class="svc-panel__head">我的订单</div>
        <ul class="svc-orders">
          <li v-for="o in orders" :key="o.id" class="svc-order" @click="router.push(`/user/orders/${o.id}`)">
            <div class="svc-order__id mono">#{{ o.id }} <el-tag size="small" :type="tagType(o.status)">{{ statusLabel(o.status) }}</el-tag></div>
            <div class="svc-order__meta mono">¥{{ fmtMoney(o.amount) }} · {{ o.createdAt?.slice(0, 10) }}</div>
          </li>
        </ul>
        <el-empty v-if="!orders.length" description="暂无订单" />
      </section>

      <!-- 订单评论 -->
      <section class="svc-panel glass">
        <div class="svc-panel__head">订单评论</div>
        <div class="svc-review">
          <div class="svc-review__row">
            <span class="svc-label">订单</span>
            <el-select v-model="reviewOrder" placeholder="选择订单" class="svc-review__sel">
              <el-option v-for="o in reviewableOrders" :key="o.id" :label="'#' + o.id" :value="o.id" />
            </el-select>
          </div>
          <div class="svc-review__row">
            <span class="svc-label">评分</span>
            <el-rate v-model="reviewRating" />
          </div>
          <el-input v-model="reviewContent" type="textarea" :rows="2" placeholder="对这次配送的评价…" class="svc-review__input" />
          <el-button type="primary" size="small" :loading="reviewing" @click="submitReview">提交评论</el-button>
        </div>
      </section>

      <!-- 留言反馈 -->
      <section class="svc-panel glass">
        <div class="svc-panel__head">留言反馈</div>
        <div class="svc-feedback">
          <el-select v-model="fbType" class="svc-feedback__sel">
            <el-option label="投诉" value="COMPLAIN" />
            <el-option label="建议" value="SUGGEST" />
            <el-option label="咨询" value="CONSULT" />
          </el-select>
          <el-input v-model="fbContent" type="textarea" :rows="2" placeholder="写下您的意见…" class="svc-feedback__input" />
          <el-button type="primary" size="small" :loading="fbSending" @click="submitFb">提交反馈</el-button>
        </div>
      </section>

      <!-- 客服 IM -->
      <section class="svc-panel glass">
        <div class="svc-panel__head">在线客服</div>
        <div v-if="imSession != null" class="svc-im">
          <div class="svc-im__msgs">
            <div v-for="m in imMsgs" :key="m.id" class="svc-im__msg" :class="{ me: m.senderRole === 'USER' }">
              <span class="mono svc-im__role">{{ m.senderRole === 'USER' ? '我' : '客服' }}</span>
              <span>{{ m.content }}</span>
            </div>
            <el-empty v-if="!imMsgs.length" description="开始对话" :image-size="60" />
          </div>
          <el-input v-model="imInput" placeholder="输入消息…" @keyup.enter="sendIm" />
          <el-button type="primary" size="small" @click="sendIm">发送</el-button>
        </div>
        <div v-else class="svc-im__empty">
          <el-button @click="openIm">开启客服会话</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import type { Order } from '@/types'

const router = useRouter()
const orders = ref<Order[]>([])
const reviewOrder = ref<number | null>(null)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewing = ref(false)
const fbType = ref('COMPLAIN')
const fbContent = ref('')
const fbSending = ref(false)
const imSession = ref<number | null>(null)
const imMsgs = ref<{ id: number; senderRole: string; content: string; sentAt: string }[]>([])
const imInput = ref('')

const reviewableOrders = computed(() => orders.value.filter((o) => o.status === 'DELIVERED'))

function fmtMoney(fen: number) { return (fen / 100).toFixed(2) }
function statusLabel(st: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已取件', IN_TRANSIT: '配送中',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消',
  }
  return map[st] ?? st
}
function tagType(st: string) {
  if (st === 'DELIVERED' || st === 'ARRIVED') return 'success'
  if (st === 'IN_TRANSIT' || st === 'PICKED') return 'warning'
  if (st === 'CANCELLED') return 'danger'
  return 'info'
}



onMounted(async () => {
  try {
    const p = await userApi.myOrders(undefined, 0, 50)
    orders.value = p.content
    reviewOrder.value = reviewableOrders.value[0]?.id ?? null
  } catch (e) { console.error(e) }
})

async function submitReview() {
  if (!reviewOrder.value) return ElMessage.warning('请选择订单')
  reviewing.value = true
  try {
    await userApi.submitReview(reviewOrder.value, reviewRating.value, reviewContent.value)
    ElMessage.success('评论已提交')
    reviewContent.value = ''
  } catch (e) { ElMessage.error('提交失败') }
  reviewing.value = false
}

async function submitFb() {
  if (!fbContent.value.trim()) return ElMessage.warning('请填写内容')
  fbSending.value = true
  try {
    await userApi.submitFeedback(fbType.value, fbContent.value)
    ElMessage.success('反馈已提交')
    fbContent.value = ''
  } catch { ElMessage.error('提交失败') }
  fbSending.value = false
}

async function openIm() {
  try {
    const s = await userApi.openIm()
    imSession.value = s.id
    imMsgs.value = await userApi.imHistory(s.id)
  } catch { ElMessage.error('开启会话失败') }
}

async function sendIm() {
  if (!imInput.value.trim() || imSession.value == null) return
  try {
    await userApi.sendIm(imSession.value, imInput.value)
    imMsgs.value.push({ id: Date.now(), senderRole: 'USER', content: imInput.value, sentAt: new Date().toISOString() })
    imInput.value = ''
  } catch { ElMessage.error('发送失败') }
}
</script>

<style scoped>
.svc-wrap { min-height: calc(100vh - 108px); display: flex; flex-direction: column; gap: 16px; }
.svc-head { display: flex; align-items: baseline; gap: 10px; }
.svc-head__title { position: relative; padding-left: 14px; }
.svc-head__title::before {
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
.svc-title { margin: 0; font-size: 24px; font-weight: 700; color: #1e3a5f; }
.svc-sub { font-size: 12px; color: var(--text-mid); }
.svc-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
@media (max-width: 900px) { .svc-grid { grid-template-columns: 1fr; } }
.svc-panel { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.svc-panel__head { font-size: 14px; font-weight: 700; color: #1e3a5f; padding-bottom: 10px; border-bottom: 1px solid var(--glass-border); display: flex; align-items: center; gap: 8px; }
.svc-panel__head::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: #d98e2b; box-shadow: 0 0 0 2.5px rgba(217, 142, 43, 0.22); }
.svc-orders { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.svc-order { padding: 10px 12px; border: 1px solid var(--glass-border); border-radius: var(--radius-sm); cursor: pointer; }
.svc-order:hover { border-color: var(--primary); }
.svc-order__id { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.svc-order__meta { font-size: 11px; color: var(--text-low); margin-top: 4px; }
.svc-review { display: flex; flex-direction: column; gap: 10px; }
.svc-review__row { display: flex; align-items: center; gap: 10px; }
.svc-review__sel { width: 140px; }
.svc-label { font-size: 12px; color: var(--text-mid); }
.svc-review__input { margin-bottom: 6px; }
.svc-feedback { display: flex; flex-direction: column; gap: 10px; }
.svc-feedback__sel { width: 120px; }
.svc-im { display: flex; flex-direction: column; gap: 10px; }
.svc-im__msgs { max-height: 240px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.svc-im__msg { padding: 8px 10px; border-radius: var(--radius-sm); background: var(--glass-bg-2); font-size: 13px; }
.svc-im__msg.me { background: rgba(79, 140, 255, 0.16); align-self: flex-end; }
.svc-im__role { font-size: 10px; color: var(--text-low); margin-right: 8px; }
.svc-im__empty { padding: 20px; text-align: center; color: var(--text-mid); }
</style>
