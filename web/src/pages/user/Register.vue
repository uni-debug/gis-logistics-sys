<template>
  <div class="reg-wrap">
    <div class="reg-bg">
      <div class="reg-bg__grid"></div>
      <div class="reg-bg__glow"></div>
    </div>

    <div class="reg-panel glass">
      <div class="reg-panel__head">
        <button class="reg-back" @click="goBack">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M15 6l-6 6 6 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回登录
        </button>
        <div class="reg-panel__titles">
          <div class="reg-panel__brand mono">GIS&nbsp;LOGISTICS</div>
          <h1 class="reg-panel__title">创建账号</h1>
          <p class="reg-panel__sub">注册后默认进入<b>用户端</b>，可下单、追踪轨迹、评价反馈</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        class="reg-panel__form"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="你的姓名" :aria-label="'姓名'" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" :aria-label="'手机号'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-20 位密码" show-password maxlength="20" :aria-label="'密码'" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="再次输入密码" show-password maxlength="20" :aria-label="'确认密码'" />
        </el-form-item>
        <el-button
          type="primary"
          class="reg-submit"
          :loading="loading"
          @click="onSubmit"
        >
          注 册
        </el-button>
      </el-form>

      <div class="reg-panel__foot mono">
        注册即代表同意服务条款与隐私政策
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()

function goBack() {
  if (window.history.state && window.history.state.back) router.back()
  else router.push('/user/login')
}
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref({ name: '', phone: '', password: '', confirm: '' })

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.value.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = ref<FormRules>({
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '6-20 位字符', trigger: 'blur' },
  ],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
})

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authApi.register(form.value.phone, form.value.name, form.value.password)
      ElMessage.success('注册成功，请登录')
      router.push('/user/login')
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '注册失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.reg-wrap {
  position: relative;
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  overflow: hidden;
}

.reg-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.reg-bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(22, 163, 74, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(22, 163, 74, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse 80% 65% at 50% 40%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 80% 65% at 50% 40%, #000 30%, transparent 75%);
}
.reg-bg__glow {
  position: absolute;
  width: 420px;
  height: 420px;
  left: 50%;
  top: 8%;
  transform: translateX(-50%);
  border-radius: 50%;
  background: rgba(22, 163, 74, 0.14);
  filter: blur(90px);
  opacity: 0.7;
}

.reg-panel {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 30px 28px 26px;
  box-shadow: var(--shadow-glow);
}

.reg-panel__head {
  margin-bottom: 22px;
}
.reg-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px solid var(--glass-border);
  color: var(--text-mid);
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.18s var(--ease), border-color 0.18s var(--ease), background 0.18s var(--ease);
}
.reg-back:hover {
  color: var(--text-hi);
  border-color: var(--glass-border-2);
  background: var(--glass-bg-2);
}
.reg-back:focus-visible {
  outline: 2px solid var(--success);
  outline-offset: 2px;
}

.reg-panel__brand {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--primary);
  margin-bottom: 6px;
}
.reg-panel__titles { margin-top: 18px; }
.reg-panel__title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
}
.reg-panel__sub {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--text-mid);
}
.reg-panel__sub b {
  color: var(--accent-bright);
  font-weight: 600;
}

.reg-panel__form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reg-submit {
  width: 100%;
  margin-top: 10px;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2em;
  border: none;
  background: linear-gradient(135deg, var(--success), #059669);
  box-shadow: 0 10px 26px rgba(22, 163, 74, 0.32);
  transition: transform 0.15s var(--ease), box-shadow 0.2s var(--ease);
}
.reg-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(22, 163, 74, 0.42);
}
.reg-submit:focus-visible {
  outline: 2px solid var(--accent-bright);
  outline-offset: 2px;
}

.reg-panel__foot {
  margin-top: 20px;
  text-align: center;
  font-size: 11px;
  color: var(--text-low);
  letter-spacing: 0.05em;
}

@media (max-width: 480px) {
  .reg-panel { padding: 24px 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .reg-submit, .reg-back { transition: none; }
}
</style>