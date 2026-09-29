<template>
  <div class="profile-wrap">
    <div class="profile-head">
      <div class="profile-head__title">
        <h1 class="profile-title">个人中心</h1>
        <p class="profile-sub">账号资料、安全设置与全部操作记录</p>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 资料 -->
      <section class="profile-panel glass">
        <div class="profile-panel__head">
          <span class="profile-panel__title">账号资料</span>
          <el-button size="small" type="primary" plain :loading="saving" @click="saveProfile">保存</el-button>
        </div>
        <el-form label-position="top" class="profile-form">
          <el-form-item label="姓名">
            <el-input v-model="profileName" placeholder="请输入姓名" maxlength="64" />
          </el-form-item>
          <el-form-item label="手机号（已加密存储，仅展示脱敏）">
            <el-input :model-value="phoneMasked" disabled />
          </el-form-item>
          <el-form-item label="注册角色">
            <el-input :model-value="roleLabel" disabled />
          </el-form-item>
        </el-form>
      </section>

      <!-- 修改密码 -->
      <section class="profile-panel glass">
        <div class="profile-panel__head">
          <span class="profile-panel__title">修改密码</span>
        </div>
        <el-form label-position="top" class="profile-form">
          <el-form-item label="原密码">
            <el-input v-model="oldPwd" type="password" show-password placeholder="原密码" />
          </el-form-item>
          <el-form-item label="新密码（至少 6 位）">
            <el-input v-model="newPwd" type="password" show-password placeholder="新密码" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="confirmPwd" type="password" show-password placeholder="再次输入新密码" />
          </el-form-item>
          <el-button type="primary" :loading="changingPwd" @click="changePassword">确认修改</el-button>
        </el-form>
      </section>

      <!-- 申请成为员工 -->
      <section class="profile-panel glass" v-if="profileRole === 'USER'">
        <div class="profile-panel__head">
          <span class="profile-panel__title">申请成为员工</span>
          <el-button v-if="appStatus === 'PENDING'" size="small" type="warning" plain @click="cancelApplication">撤回申请</el-button>
          <el-tag v-else-if="appStatus === 'APPROVED'" type="success" size="small">已通过</el-tag>
          <el-tag v-else-if="appStatus === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
        </div>
        <div v-if="appStatus === 'PENDING'">
          <p class="profile-app-hint">申请已提交（#{{ appId }}），等待管理员审批，可撤回后重新申请。</p>
        </div>
        <div v-else-if="appStatus === 'APPROVED'">
          <p class="profile-app-hint">你已成功成为员工，可使用员工端登录。</p>
        </div>
        <div v-else-if="appStatus === 'REJECTED'">
          <p class="profile-app-hint">申请被拒绝：{{ appReason || '未填写原因' }}。可重新提交。</p>
          <button class="cta-btn cta-btn--ghost profile-app-resubmit" @click="resetForm">重新申请</button>
        </div>
        <el-form v-if="!appStatus || appStatus === 'REJECTED'" label-position="top" class="profile-form">
          <el-form-item label="期望站点">
            <el-select v-model="staffAppForm.siteId" placeholder="选择站点" style="width:100%">
              <el-option v-for="site in sites" :key="site.id" :label="site.name" :value="site.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="执业资质编号">
            <el-input v-model="staffAppForm.licenseNo" placeholder="请输入执业资质编号" maxlength="64" />
          </el-form-item>
          <el-form-item label="申请说明（可选）">
            <el-input v-model="staffAppForm.reason" type="textarea" :rows="2" placeholder="简要说明申请理由" maxlength="512" />
          </el-form-item>
          <el-button type="primary" :loading="appSubmitting" @click="submitApplication">提交申请</el-button>
        </el-form>
      </section>

      <!-- 账单 / 评论 / 反馈记录 -->
      <section class="profile-panel profile-panel--full glass">
        <div class="profile-panel__head">
          <span class="profile-panel__title">记录中心</span>
        </div>
        <div class="profile-tabs">
          <button
            v-for="t in tabs"
            :key="t.key"
            class="profile-tab"
            :class="{ 'profile-tab--active': tab === t.key }"
            @click="switchTab(t.key)"
          >{{ t.label }}</button>
        </div>

        <div v-if="tab === 'orders'" class="profile-list">
          <div v-for="o in orders" :key="o.id" class="profile-row">
            <span class="mono">订单 #{{ o.id }}</span>
            <el-tag size="small">{{ statusLabel(o.status) }}</el-tag>
            <span class="mono profile-row__meta">¥{{ fmtMoney(o.amount) }} · {{ o.createdAt?.slice(0, 10) }}</span>
          </div>
          <div v-if="orders.length === 0" class="profile-empty">暂无订单记录</div>
        </div>

        <div v-if="tab === 'reviews'" class="profile-list">
          <div v-for="r in reviews" :key="r.id" class="profile-row">
            <span class="mono">订单 #{{ r.orderId }}</span>
            <span class="profile-row__stars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</span>
            <span class="profile-row__meta">{{ r.content ?? '（无文字评价）' }}</span>
          </div>
          <div v-if="reviews.length === 0" class="profile-empty">暂无评论记录</div>
        </div>

        <div v-if="tab === 'feedback'" class="profile-list">
          <div v-for="f in feedbacks" :key="f.id" class="profile-row">
            <el-tag size="small" :type="f.status === 'CLOSED' ? 'info' : 'warning'">{{ f.status }}</el-tag>
            <span class="profile-row__meta">{{ f.content }}</span>
            <span v-if="f.reply" class="profile-row__reply">回复：{{ f.reply }}</span>
          </div>
          <div v-if="feedbacks.length === 0" class="profile-empty">暂无反馈记录</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/user'
import { authApi } from '@/api/auth'
import type { Order, Review } from '@/types'

const profileName = ref('')
const phoneMasked = ref('')
const roleLabel = ref('')
const profileRole = ref('USER')
const saving = ref(false)
const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const changingPwd = ref(false)

// 员工申请
const appStatus = ref<string | null>(null)
const appId = ref<number | undefined>(undefined)
const appReason = ref<string | null>(null)
const appSubmitting = ref(false)
const sites = ref<{ id: number; name: string }[]>([])
const staffAppForm = reactive({ siteId: undefined as number | undefined, licenseNo: '', reason: '' })

async function loadApplication() {
  try {
    const app = await userApi.myStaffApplication()
    if (app) {
      appStatus.value = app.status
      appId.value = app.id
      appReason.value = app.reason ?? null
      if (app.reason) staffAppForm.reason = app.reason
      if (app.licenseNo) staffAppForm.licenseNo = app.licenseNo
    }
  } catch { /* 未登录或无申请 */ }
}

async function loadSites() {
  try {
    const res = await userApi.sites()
    sites.value = res
  } catch { sites.value = [] }
}

async function submitApplication() {
  if (!staffAppForm.siteId || !staffAppForm.licenseNo.trim()) {
    ElMessage.warning('请填写站点和资质编号')
    return
  }
  appSubmitting.value = true
  try {
    const app = await userApi.submitStaffApplication({
      siteId: staffAppForm.siteId,
      licenseNo: staffAppForm.licenseNo.trim(),
      reason: staffAppForm.reason || undefined,
    })
    appStatus.value = app.status
    appId.value = app.id
    ElMessage.success('申请已提交，请等待管理员审批')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败')
  } finally {
    appSubmitting.value = false
  }
}

async function cancelApplication() {
  try {
    await userApi.cancelStaffApplication()
    appStatus.value = null
    appId.value = undefined
    appReason.value = null
    ElMessage.success('申请已撤回，可重新提交')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '撤回失败')
  }
}

function resetForm() {
  appStatus.value = null
  appId.value = undefined
  appReason.value = null
}

type TabKey = 'orders' | 'reviews' | 'feedback'
const tab = ref<TabKey>('orders')
const tabs: { key: TabKey; label: string }[] = [
  { key: 'orders', label: '订单记录' },
  { key: 'reviews', label: '评论记录' },
  { key: 'feedback', label: '反馈记录' },
]

const orders = ref<Order[]>([])
const reviews = ref<Review[]>([])
const feedbacks = ref<{ id: number; type: string; content: string; status: string; reply: string | null }[]>([])

function fmtMoney(fen: number): string {
  return (fen / 100).toFixed(2)
}
function statusLabel(s: string): string {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', PICKED: '已揽件', IN_TRANSIT: '在途',
    ARRIVED: '已到达', DELIVERED: '已签收', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
  }
  return map[s] ?? s
}

function loadRecords() {
  userApi.myOrders(undefined, 0, 50).then((p) => (orders.value = p.content)).catch(() => {})
  authApi.myReviews().then((rs) => (reviews.value = rs)).catch(() => {})
  authApi.myFeedbacks().then((fs) => (feedbacks.value = fs)).catch(() => {})
}

function switchTab(key: TabKey) {
  tab.value = key
}

async function loadProfile() {
  const p = await authApi.profile()
  profileName.value = p.name ?? ''
  phoneMasked.value = p.phoneMasked ?? ''
  roleLabel.value = p.role === 'USER' ? '用户' : p.role === 'STAFF' ? '员工' : '管理员'
  profileRole.value = p.role
}

async function saveProfile() {
  if (!profileName.value.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    await authApi.updateProfile(profileName.value.trim())
    ElMessage.success('资料已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (!oldPwd.value || !newPwd.value || !confirmPwd.value) {
    ElMessage.warning('请填写完整')
    return
  }
  if (newPwd.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (newPwd.value !== confirmPwd.value) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPwd.value = true
  try {
    await authApi.changePassword(oldPwd.value, newPwd.value)
    ElMessage.success('密码已修改，请下次登录使用新密码')
    oldPwd.value = ''
    newPwd.value = ''
    confirmPwd.value = ''
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    changingPwd.value = false
  }
}

onMounted(async () => {
  loadRecords()
  loadApplication()
  loadSites()
  try { await loadProfile() } catch { /* 未登录场景忽略 */ }
})
</script>

<style scoped>
.profile-wrap {
  max-width: 860px;
  margin: 0 auto;
  padding: 28px 20px 60px;
}
.profile-head { margin-bottom: 20px; }
.profile-head__title { position: relative; padding-left: 14px; }
.profile-head__title::before {
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
.profile-title { margin: 0; font-size: 24px; font-weight: 700; color: #1e3a5f; }
.profile-sub { margin: 6px 0 0; font-size: 12px; color: var(--text-mid); }
.profile-panel__title { color: #1e3a5f; }
.profile-row__stars { color: #d98e2b; letter-spacing: 0.1em; }

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.profile-panel { padding: 20px 22px; }
.profile-panel--full { grid-column: 1 / -1; }
.profile-panel__head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.profile-panel__title {
  font-size: 13px; font-weight: 600; letter-spacing: 0.06em; color: var(--text-mid);
}
.profile-form :deep(.el-form-item) { margin-bottom: 14px; }
.profile-form :deep(.el-form-item__label) {
  font-size: 12px; color: var(--text-mid); font-weight: 500;
}

.profile-tabs {
  display: flex; gap: 4px;
  background: var(--glass-bg-2);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 4px; margin-bottom: 14px;
}
.profile-tab {
  flex: 1;
  padding: 8px 0;
  background: none; border: none;
  font-size: 12.5px; font-weight: 500;
  color: var(--text-mid); cursor: pointer;
  transition: color 0.18s var(--ease);
  font-family: inherit;
}
.profile-tab:hover { color: var(--text-hi); }
.profile-tab--active { color: var(--text-hi); font-weight: 600; }

.profile-list { display: flex; flex-direction: column; gap: 8px; }
.profile-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  font-size: 13px;
}
.profile-row__meta { color: var(--text-mid); font-size: 12px; flex: 1; }
.profile-row__stars { color: #d98e2b; letter-spacing: 0.1em; }
.profile-row__reply {
  margin-left: auto; max-width: 40%;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-size: 12px; color: var(--text-low);
}
.profile-empty { padding: 32px 0; text-align: center; color: var(--text-low); font-size: 13px; }
.profile-app-hint {
  margin: 0 0 14px; font-size: 13px; color: var(--text-mid); line-height: 1.6;
}
.profile-app-resubmit {
  margin-top: 8px;
}


@media (max-width: 720px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
