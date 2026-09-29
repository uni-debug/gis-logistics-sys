<template>
  <div class="notices-wrap">
    <div class="notices-head">
      <div class="notices-head__title">
        <h1 class="notices-title">系统公告</h1>
        <p class="notices-sub">配送政策、时效调整、维护与优惠，第一时间掌握</p>
      </div>
    </div>

    <div v-if="loading" class="notices-empty glass">加载中…</div>

    <div v-else-if="list.length === 0" class="notices-empty glass">
      暂无已发布公告
    </div>

    <section v-for="n in list" :key="n.id" class="notice-card glass">
      <div class="notice-card__head">
        <el-tag v-if="n.pinned" size="small" type="warning">置顶</el-tag>
        <span class="notice-card__title">{{ n.title }}</span>
        <span class="notice-card__date mono">{{ fmtDate(n.createdAt) }}</span>
      </div>
      <p class="notice-card__body">{{ n.body }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/user'
import type { Notice } from '@/types'

const list = ref<Notice[]>([])
const loading = ref(true)

function fmtDate(s: string): string {
  return s?.slice(0, 10) ?? ''
}

onMounted(async () => {
  try {
    list.value = await userApi.notices()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.notices-wrap {
  max-width: 760px;
  margin: 0 auto;
  padding: 28px 20px 60px;
}
.notices-head {
  margin-bottom: 20px;
}
.notices-head__title { position: relative; padding-left: 14px; }
.notices-head__title::before {
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
.notices-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1e3a5f;
  letter-spacing: 0.01em;
}
.notices-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-mid);
}
.notices-empty {
  padding: 48px 0;
  text-align: center;
  color: var(--text-mid);
  font-size: 14px;
}
.notice-card {
  padding: 18px 22px;
  margin-bottom: 14px;
}
.notice-card {
  position: relative;
  padding-left: 22px;
}
.notice-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 2px;
  background: rgba(30, 58, 95, 0.18);
}
.notice-card:hover::before { background: #d98e2b; }
.notice-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.notice-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #1e3a5f;
  flex: 1;
}
.notice-card__date {
  font-size: 11px;
  color: var(--text-low);
}
.notice-card__body {
  margin: 0;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text-mid);
  white-space: pre-wrap;
}
</style>
