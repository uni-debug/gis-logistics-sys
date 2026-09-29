<template>
  <div class="app-shell">
    <header class="app-shell__bar">
      <div class="app-shell__brand">
        <button
          v-if="showBack"
          class="app-shell__back"
          @click="goBack()"
          aria-label="返回上级"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <span class="app-shell__pulse" :class="{ live: live }"></span>
        <span class="app-shell__title mono">GIS / LOGISTICS</span>
        <span class="app-shell__subtitle">跨区域配送控制室</span>
      </div>
      <div class="app-shell__status" :class="{ live }">
        <span class="status-dot" :class="live ? 'status-dot--live' : 'status-dot--stale'"></span>
        <span class="mono">{{ live ? 'LIVE' : 'OFFLINE' }}</span>
      </div>
    </header>
    <main class="app-shell__body">
      <div class="app-shell__page" :class="'page-' + pageKey" :key="pageKey">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const live = ref(false)
const pageKey = computed(() => route.path.replace(/\//g, '_').slice(1) || 'root')

// 非首页/非登录页显示返回按钮
const showBack = computed(() => {
  const p = window.location.pathname
  return p !== '/' && p !== '/user/login' && p !== '/user/register'
})

function goBack() {
  if (window.history.state && window.history.state.back) {
    router.back()
    return
  }
  // 首屏直达（无浏览器历史）：按当前角色 fallback 到对应首页
  const p = window.location.pathname
  router.push(p.startsWith('/admin') ? '/admin' : p.startsWith('/staff') ? '/staff' : '/user')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-shell__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px clamp(20px, 4vw, 48px);
  background: var(--glass-bg-2);
  border-bottom: 1px solid var(--glass-border);
  backdrop-filter: var(--glass-blur);
  position: sticky;
  top: 0;
  z-index: 20;
}
.app-shell__bar::before {
  content: "";
  display: none;
}

.app-shell__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-shell__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--glass-border-2);
  color: var(--text-hi);
  border-radius: var(--radius-sm);
  padding: 4px 7px;
  cursor: pointer;
  transition: color 0.18s var(--ease), border-color 0.18s var(--ease), background 0.18s var(--ease);
}
.app-shell__back svg { display: block; }

.app-shell__back:hover {
  color: var(--text-hi);
  border-color: var(--glass-border-2);
}

.app-shell__pulse {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 12px var(--primary);
}

.app-shell__pulse.live {
  background: var(--success);
  box-shadow: 0 0 12px var(--success);
}

.app-shell__title {
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.08em;
}

.app-shell__subtitle {
  font-size: 12px;
  color: var(--text-mid);
}

.app-shell__status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-mid);
}

.app-shell__body {
  flex: 1;
  padding: 24px clamp(20px, 4vw, 48px) 40px;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

/* 页面切换：轻量转场（不引入额外依赖） */
.app-shell__page {
  animation: page-in 0.28s var(--ease);
}

@keyframes page-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-shell__page {
    animation: none;
  }
}
</style>
