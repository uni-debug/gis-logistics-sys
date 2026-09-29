<template>
  <div class="login-wrap">
    <div class="login-bg" aria-hidden="true">
      <div class="login-bg__vignette"></div>
      <div class="login-bg__grid"></div>
      <!-- 浅色科技城市地图：斜切网格 + 街区 + 路线 + 数据点 -->
      <div class="login-bg__city-map" aria-hidden="true"></div>
      <span class="login-bg__marker" style="left: 46%; top: 38%"></span>
      <span class="login-bg__marker" style="left: 72%; top: 42%"></span>
      <svg class="login-bg__roads" viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid slice">
    <g transform="translate(-110 40)">
    <g class="login-bg__region" fill="rgba(30, 58, 95, 0.045)" stroke="rgba(30, 58, 95, 0.20)" stroke-width="0.5">
      <path d="M285 78 C 395 42, 545 58, 640 118 C 745 168, 775 268, 706 352 C 616 412, 420 418, 322 356 C 224 292, 188 152, 285 78 Z"/>
      <path d="M392 522 C 540 480, 730 492, 848 560 C 906 620, 872 712, 748 750 C 596 788, 430 760, 362 672 C 318 606, 328 548, 392 522 Z"/>
    </g>
    <g class="login-bg__trunk" fill="none" stroke="rgba(30, 58, 95, 0.14)" stroke-width="0.8" stroke-linecap="round">
      <path class="login-bg__trunk--flow" d="M388 196 C 470 300, 500 380, 560 470 S 700 570, 760 610"/>
      <path d="M388 196 C 420 420, 480 560, 540 700"/>
      <path d="M560 470 C 640 470, 700 490, 780 520"/>
      <path d="M250 430 C 350 420, 430 440, 560 470"/>
      <path class="login-bg__trunk--flow" d="M760 610 C 840 630, 940 620, 1040 580"/>
      <path d="M388 196 C 330 250, 300 300, 285 372"/>
      <path class="login-bg__trunk--flow" d="M388 196 C 520 220, 650 260, 800 300"/>
      <path d="M760 610 C 720 680, 700 730, 700 790"/>
    </g>
    <g class="login-bg__branch" fill="none" stroke="rgba(30, 58, 95, 0.14)" stroke-width="0.5">
      <path d="M330 140 C 360 165, 380 180, 388 196"/>
      <path d="M470 160 C 450 180, 420 190, 388 196"/>
      <path d="M560 130 C 540 230, 555 350, 560 470"/>
      <path d="M650 340 C 610 390, 590 430, 560 470"/>
      <path d="M560 470 C 580 550, 590 600, 600 660"/>
      <path d="M700 400 C 720 480, 740 550, 760 610"/>
      <path d="M760 610 C 790 560, 820 520, 860 480"/>
      <path d="M760 610 C 700 590, 640 580, 560 470"/>
      <path d="M388 196 C 260 220, 180 260, 120 320"/>
      <path d="M388 196 C 500 150, 620 130, 740 150"/>
      <path d="M560 470 C 480 450, 400 450, 330 465"/>
      <path d="M560 470 C 640 420, 720 400, 800 390"/>
      <path d="M760 610 C 830 660, 900 700, 960 740"/>
      <path d="M760 610 C 660 650, 580 680, 500 700"/>
    </g>
    <g class="login-bg__nodes" fill="rgba(30, 58, 95, 0.55)">
      <circle cx="388" cy="196" r="3"/>
      <circle cx="560" cy="470" r="3"/>
      <circle cx="760" cy="610" r="3"/>
    </g>
    <g class="login-bg__node-labels" font-family="var(--font-mono)" font-size="10" fill="rgba(30, 58, 95, 0.62)" letter-spacing="0.12em">
      <text x="396" y="192">01</text>
      <text x="568" y="466">02</text>
      <text x="768" y="606">03</text>
    </g>
    <g class="login-bg__tech" stroke="rgba(30, 58, 95, 0.16)" stroke-width="0.6" fill="none">
      <polygon points="560,390 585,405 585,435 560,450 535,435 535,405" opacity="0.55"/>
      <polygon points="760,530 780,542 780,566 760,578 740,566 740,542" opacity="0.45"/>
      <polygon points="388,300 402,309 402,327 388,336 374,327 374,309" opacity="0.4"/>
      <circle cx="560" cy="420" r="2" fill="rgba(30, 58, 95, 0.30)" stroke="none"/>
      <circle cx="760" cy="554" r="2" fill="rgba(30, 58, 95, 0.26)" stroke="none"/>
      <circle cx="388" cy="318" r="1.6" fill="rgba(30, 58, 95, 0.22)" stroke="none"/>
    </g>
    <g class="login-bg__radar" transform="translate(560 470)" stroke="rgba(30, 58, 95, 0.20)" fill="none" stroke-width="0.5">
      <circle r="36" stroke-dasharray="2 6"/>
      <circle r="18" stroke-dasharray="1 5" opacity="0.7"/>
      <line x1="0" y1="0" x2="32" y2="0" class="login-bg__radar-sweep" stroke="rgba(30, 58, 95, 0.32)" stroke-width="0.8"/>
    </g>
    <g class="login-bg__cities" fill="rgba(30, 58, 95, 0.70)">
      <circle cx="310" cy="260" r="2.4"/>
      <circle cx="445" cy="330" r="2.4"/>
      <circle cx="615" cy="360" r="2.4"/>
      <circle cx="660" cy="540" r="2.4"/>
      <circle cx="850" cy="470" r="2.4"/>
      <circle cx="250" cy="430" r="1.8"/>
      <circle cx="490" cy="580" r="1.8"/>
      <circle cx="700" cy="700" r="1.8"/>
      <circle cx="920" cy="560" r="1.8"/>
    </g>
    <g class="login-bg__city-rings" fill="none" stroke="rgba(30, 58, 95, 0.42)" stroke-width="0.7">
      <circle cx="310" cy="260" r="5"/>
      <circle cx="445" cy="330" r="5"/>
      <circle cx="615" cy="360" r="5"/>
      <circle cx="660" cy="540" r="5"/>
      <circle cx="850" cy="470" r="5"/>
    </g>
    <g class="login-bg__city-labels" font-family="var(--font-mono)" font-size="10" fill="rgba(30, 58, 95, 0.80)" letter-spacing="0.08em">
      <text x="316" y="254">A</text>
      <text x="451" y="324">B</text>
      <text x="621" y="354">C</text>
      <text x="666" y="534">D</text>
      <text x="856" y="464">E</text>
      <text x="256" y="424" fill="rgba(30, 58, 95, 0.58)">F</text>
      <text x="496" y="574" fill="rgba(30, 58, 95, 0.58)">G</text>
      <text x="706" y="694" fill="rgba(30, 58, 95, 0.58)">H</text>
      <text x="926" y="554" fill="rgba(30, 58, 95, 0.58)">I</text>
    </g>
    <g class="login-bg__ruler" stroke="rgba(30, 58, 95, 0.12)" stroke-width="0.6">
      <line x1="60" y1="0" x2="60" y2="36"/>
      <line x1="60" y1="0" x2="60" y2="36" stroke="rgba(30, 58, 95, 0.20)"/>
      <line x1="60" y1="0" x2="1140" y2="0" stroke="rgba(30, 58, 95, 0.14)"/>
      <line x1="60" y1="796" x2="1140" y2="796" stroke="rgba(30, 58, 95, 0.14)"/>
      <line x1="1140" y1="760" x2="1140" y2="800"/>
      <line x1="1140" y1="760" x2="1140" y2="800" stroke="rgba(30, 58, 95, 0.20)"/>
      <g>
        <line x1="60" y1="0" x2="60" y2="12"/>
        <line x1="120" y1="0" x2="120" y2="8"/>
        <line x1="180" y1="0" x2="180" y2="8"/>
        <line x1="240" y1="0" x2="240" y2="12"/>
        <line x1="300" y1="0" x2="300" y2="8"/>
        <line x1="360" y1="0" x2="360" y2="8"/>
        <line x1="420" y1="0" x2="420" y2="12"/>
        <line x1="480" y1="0" x2="480" y2="8"/>
        <line x1="540" y1="0" x2="540" y2="8"/>
        <line x1="600" y1="0" x2="600" y2="12"/>
        <line x1="660" y1="0" x2="660" y2="8"/>
        <line x1="720" y1="0" x2="720" y2="8"/>
        <line x1="780" y1="0" x2="780" y2="12"/>
        <line x1="840" y1="0" x2="840" y2="8"/>
        <line x1="900" y1="0" x2="900" y2="8"/>
        <line x1="960" y1="0" x2="960" y2="12"/>
        <line x1="1020" y1="0" x2="1020" y2="8"/>
        <line x1="1080" y1="0" x2="1080" y2="8"/>
        <line x1="1140" y1="0" x2="1140" y2="12"/>
      </g>
      <g>
        <line x1="60" y1="800" x2="60" y2="788"/>
        <line x1="120" y1="800" x2="120" y2="792"/>
        <line x1="180" y1="800" x2="180" y2="792"/>
        <line x1="240" y1="800" x2="240" y2="788"/>
        <line x1="300" y1="800" x2="300" y2="792"/>
        <line x1="360" y1="800" x2="360" y2="792"/>
        <line x1="420" y1="800" x2="420" y2="788"/>
        <line x1="480" y1="800" x2="480" y2="792"/>
        <line x1="540" y1="800" x2="540" y2="792"/>
        <line x1="600" y1="800" x2="600" y2="788"/>
        <line x1="660" y1="800" x2="660" y2="792"/>
        <line x1="720" y1="800" x2="720" y2="792"/>
        <line x1="780" y1="800" x2="780" y2="788"/>
        <line x1="840" y1="800" x2="840" y2="792"/>
        <line x1="900" y1="800" x2="900" y2="792"/>
        <line x1="960" y1="800" x2="960" y2="788"/>
        <line x1="1020" y1="800" x2="1020" y2="792"/>
        <line x1="1080" y1="800" x2="1080" y2="792"/>
        <line x1="1140" y1="800" x2="1140" y2="788"/>
      </g>
    </g>
    </g>
    <g class="login-bg__scale">
      <line x1="980" y1="700" x2="1100" y2="700" stroke="rgba(30, 58, 95, 0.42)" stroke-width="1"/>
      <line x1="980" y1="694" x2="980" y2="706" stroke="rgba(30, 58, 95, 0.42)" stroke-width="1"/>
      <line x1="1040" y1="696" x2="1040" y2="704" stroke="rgba(30, 58, 95, 0.30)" stroke-width="0.8"/>
      <line x1="1100" y1="694" x2="1100" y2="706" stroke="rgba(30, 58, 95, 0.42)" stroke-width="1"/>
      <text x="980" y="688" font-family="var(--font-mono)" font-size="8" fill="rgba(30, 58, 95, 0.55)">0</text>
      <text x="1040" y="688" font-family="var(--font-mono)" font-size="8" fill="rgba(30, 58, 95, 0.38)" text-anchor="middle">100</text>
      <text x="1100" y="688" font-family="var(--font-mono)" font-size="8" fill="rgba(30, 58, 95, 0.55)" text-anchor="end">200km</text>
    </g>
    <g class="login-bg__coords" font-family="var(--font-mono)" font-size="10" fill="rgba(30, 58, 95, 0.42)">
      <text x="60" y="58">39°N 116°E</text>
      <text x="60" y="778">34°N 117°E</text>
      <text x="1140" y="58" text-anchor="end">50°N 125°E</text>
      <text x="1140" y="778" text-anchor="end">31°N 121°E</text>
    </g>
  </svg>
      <div class="login-bg__focus" :class="{ 'login-bg__focus--active': cardOpen }"></div>
    </div>

    <div class="login-console" aria-hidden="true">
      <span class="login-console__tl"></span>
      <span class="login-console__tick login-console__tick--h"></span>
      <span class="login-console__br"></span>
    </div>

    <div class="login-shell" :class="{ 'login-shell--hidden': !cardOpen }">
      <!-- 左侧品牌区 -->
      <aside class="login-brand">
        <svg class="login-brand__route" viewBox="0 0 200 80" fill="none" aria-hidden="true">
          <path
            d="M21 38 C 64 22, 92 48, 110 46 S 144 32, 163 35"
            stroke="rgba(30, 58, 95, 0.4)"
            stroke-width="0.9"
            stroke-dasharray="2 3"
            stroke-linecap="round"
            class="login-brand__route-line"
          />
          <g fill="rgba(30, 58, 95, 0.45)">
            <circle cx="21" cy="38" r="2.5"/>
            <circle cx="110" cy="46" r="2.5"/>
            <circle cx="163" cy="35" r="2.5"/>
          </g>
          <circle r="1.5" fill="rgba(30, 58, 95, 0.7)" class="login-brand__route-dot">
            <animateMotion
              dur="9s"
              repeatCount="indefinite"
              path="M21 38 C 64 22, 92 48, 110 46 S 144 32, 163 35"
            />
          </circle>
        </svg>
        <div class="login-brand__logo">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.2" opacity="0.5"/>
            <circle cx="12" cy="12" r="5.5" stroke="currentColor" stroke-width="1.2"/>
            <path d="M12 1.5v4M12 18.5v4M1.5 12h4M18.5 12h4" stroke="currentColor" stroke-width="1" opacity="0.45"/>
          </svg>
          <span class="login-brand__name">GIS&nbsp;Logistics</span>
        </div>

        <h1 class="login-brand__title">跨区域配送<br/>路线与轨迹中枢</h1>
        <p class="login-brand__sub">
          GIS 路线规划 · 实时轨迹追踪 · 订单全链路<br/>
          北京—上海—徐州 三节点示范通道已就绪
        </p>

        <div class="login-brand__metrics">
          <div class="metric">
            <span class="metric__icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M12 21s-6.2-5.4-6.2-9.8A6.2 6.2 0 0 1 12 5a6.2 6.2 0 0 1 6.2 6.2C18.2 15.6 12 21 12 21z" stroke="currentColor" stroke-width="1.5"/>
                <circle cx="12" cy="11.2" r="2.2" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </span>
            <span class="metric__val mono">1,067<small>km</small></span>
            <span class="metric__label">京沪干线距离</span>
          </div>
          <div class="metric">
            <span class="metric__icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="8.4" stroke="currentColor" stroke-width="1.5"/>
                <path d="M12 7.4V12l3.2 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </span>
            <span class="metric__val mono">≈13.5<small>h</small></span>
            <span class="metric__label">预估在途时长</span>
          </div>
          <div class="metric">
            <span class="metric__icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M4 7h16v10H4z" stroke="currentColor" stroke-width="1.5"/>
                <path d="M4 10h16" stroke="currentColor" stroke-width="1.5"/>
                <circle cx="16.6" cy="14" r="1.6" stroke="currentColor" stroke-width="1.3"/>
              </svg>
            </span>
            <span class="metric__val mono">¥2,134.62</span>
            <span class="metric__label">策略成本</span>
          </div>
        </div>
        <div class="login-brand__metric-note mono">示例指标 · 实时统计见登录后各端后台</div>

        <div class="login-brand__foot mono">v0.1 · Secure Channel · Live Tracking</div>
      </aside>

      <!-- 右侧登录区 -->
      <div class="login-panel glass">
        <!-- 角色切换入口 -->
        <div class="role-tabs" role="tablist" aria-label="登录角色">
          <button
            v-for="r in roleTabs"
            :key="r.key"
            class="role-tab"
            :class="{ 'role-tab--active': role === r.key }"
            role="tab"
            :aria-selected="role === r.key"
            @click="role = r.key"
          >
            <svg class="role-tab__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <template v-if="r.key === 'USER'">
                <circle cx="12" cy="8" r="3.4" stroke="currentColor" stroke-width="1.5"/>
                <path d="M5 20c1.4-3.4 4-5 7-5s5.6 1.6 7 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </template>
              <template v-else-if="r.key === 'STAFF'">
                <rect x="4" y="7" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <path d="M8 7V5.5A1.5 1.5 0 0 1 9.5 4h5A1.5 1.5 0 0 1 16 5.5V7" stroke="currentColor" stroke-width="1.5"/>
                <path d="M4 12h16" stroke="currentColor" stroke-width="1.5"/>
              </template>
              <template v-else>
                <path d="M12 3l7 3.5v5c0 4.4-2.9 7.7-7 9-4.1-1.3-7-4.6-7-9v-5L12 3z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
              </template>
            </svg>
            <span>{{ r.label }}</span>
          </button>
          <div class="role-tabs__indicator" :style="{ transform: `translateX(${roleIndex * 100}%)` }"></div>
        </div>

        <div class="login-panel__hint mono" :key="role">
          <span class="status-dot status-dot--live"></span>
          <span>{{ roleHint }}</span>
        </div>

        <el-form class="login-panel__form" @submit.prevent="onSubmit">
          <div class="field">
            <label class="field__label">手机号</label>
            <input
              v-model="phone"
              class="field__input"
              :aria-label="`手机号（${roleLabel}）`"
              autofocus
            />
          </div>

          <div class="field field--with-eye">
            <label class="field__label">登录密码</label>
            <input
              v-model="password"
              class="field__input"
              :type="showPwd ? 'text' : 'password'"
              :aria-label="`密码（${roleLabel}）`"
            />
            <button
              type="button"
              class="field__eye"
              :aria-label="showPwd ? '隐藏密码' : '显示密码'"
              @click="showPwd = !showPwd"
            >
              <svg v-if="!showPwd" width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6-10-6-10-6z" stroke="currentColor" stroke-width="1.5"/>
                <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.5"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M2 12s3.6-6 10-6c1.6 0 3 .3 4.3.8M22 12s-3.6 6-10 6c-1.6 0-3-.3-4.3-.8M9.5 9.8a3 3 0 0 0 4.2 4.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M3 3l18 18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>

          <div class="login-panel__extra">
            <label class="login-panel__remember">
              <input type="checkbox" v-model="remember" class="login-panel__remember-box" />
              <span>记住密码</span>
            </label>
            <button type="button" class="login-panel__forgot" @click="gotoRegister">忘记密码？</button>
          </div>

          <el-button
            type="primary"
            class="login-panel__submit"
            :loading="loading"
            @click="onSubmit"
          >
            以<em>{{ roleLabel }}</em>身份登录
            <svg class="login-panel__submit-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 12h14m-5-5 5 5-5 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </el-button>
        </el-form>

        <div class="login-panel__foot">
          <button class="login-panel__reg-btn" @click="gotoRegister">还没有账号？去注册</button>
        </div>
      </div>
    </div>

    <!-- 货车 Canvas：置于登录卡片之上，可见 -->
    <canvas ref="netCanvas" class="login-bg__net login-bg__net--over"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

// ---------- 背景节点网络（克制的物流呼吸感） ----------
const netCanvas = ref<HTMLCanvasElement | null>(null)
const cardOpen = ref(true)
let netRaf = 0
interface Node { x: number; y: number; vx: number; vy: number; r: number }
interface Truck { t: number; x: number; phase: number; scale: number }

function initNet() {
  const canvas = netCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const dpr = Math.min(window.devicePixelRatio || 1, 2)

  function resize() {
    if (!canvas) return
    canvas.width = canvas.offsetWidth * dpr
    canvas.height = canvas.offsetHeight * dpr
    ctx && (ctx.setTransform(dpr, 0, 0, dpr, 0, 0))
  }
  resize()
  window.addEventListener('resize', resize)

  const COUNT = 52
  const nodes: Node[] = Array.from({ length: COUNT }, () => ({
    x: Math.random() * (canvas.offsetWidth || 800),
    y: Math.random() * (canvas.offsetHeight || 600),
    vx: (Math.random() - 0.5) * 0.18,
    vy: (Math.random() - 0.5) * 0.18,
    r: 1.4 + Math.random() * 1.2,
  }))

  const LINK = 130

  function tick() {
    if (!canvas || !ctx) return
    const w = canvas.offsetWidth
    const h = canvas.offsetHeight
    ctx.clearRect(0, 0, w, h)

    for (const n of nodes) {
      n.x += n.vx; n.y += n.vy
      if (n.x < 0 || n.x > w) n.vx *= -1
      if (n.y < 0 || n.y > h) n.vy *= -1
    }
    // 连线
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const a = nodes[i], b = nodes[j]
        const dx = a.x - b.x, dy = a.y - b.y
        const d = Math.hypot(dx, dy)
        if (d < LINK) {
          const alpha = (1 - d / LINK) * 0.16
          ctx.strokeStyle = `rgba(96, 116, 140, ${alpha.toFixed(3)})`
          ctx.lineWidth = 1
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }
    // 节点
    for (const n of nodes) {
      ctx.fillStyle = 'rgba(96, 116, 140, 0.28)'
      ctx.beginPath()
      ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2)
      ctx.fill()
    }

    // 公路（位于登录卡片下方）+ 彩色货车
    const roadY = h * 0.72
    const roadH = 10
    ctx.strokeStyle = 'rgba(79, 98, 117, 0.16)'
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(0, roadY)
    ctx.lineTo(w, roadY)
    ctx.stroke()
    // 虚线车道线
    ctx.strokeStyle = 'rgba(79, 98, 117, 0.26)'
    ctx.lineWidth = 1
    ctx.setLineDash([14, 18])
    ctx.lineDashOffset = truck.phase
    ctx.beginPath()
    ctx.moveTo(0, roadY + roadH / 2)
    ctx.lineTo(w, roadY + roadH / 2)
    ctx.stroke()
    ctx.setLineDash([])
    drawTruck(ctx, truck.x, roadY + roadH / 2, truck.scale)
    truck.phase = (truck.phase + 0.35) % 32
    truck.t += 0.0016
    const span = w - 80
    truck.x = 40 + ((truck.t % 1) * span)
    if (!reduced) netRaf = requestAnimationFrame(tick)
  }

  // 货车初始位置
  const truck: Truck = { t: 0.28, x: 0, phase: 0, scale: 1 }
  function drawTruck(c: CanvasRenderingContext2D, x: number, y: number, scale: number) {
    c.save()
    c.translate(x, y - 9 * scale)
    c.scale(scale, scale)
    // 货箱（主色蓝灰 + 顶部高亮）
    c.fillStyle = '#4f6275'
    c.strokeStyle = '#38485a'
    c.lineWidth = 1
    roundRect(c, -24, -11, 32, 15, 2)
    c.fill()
    c.stroke()
    // 货箱高光线
    c.strokeStyle = 'rgba(255, 255, 255, 0.35)'
    c.beginPath()
    c.moveTo(-21, -8)
    c.lineTo(5, -8)
    c.stroke()
    // 车头（琥珀色点缀）
    c.fillStyle = '#d97706'
    roundRect(c, 9, -7, 12, 11, 2)
    c.fill()
    // 车窗
    c.fillStyle = 'rgba(255, 255, 255, 0.55)'
    roundRect(c, 11, -5, 7, 4, 1)
    c.fill()
    // 车轮（深灰 + 浅色轮毂）
    c.fillStyle = '#1f2430'
    c.beginPath(); c.arc(-15, 5, 3, 0, Math.PI * 2); c.fill()
    c.beginPath(); c.arc(13, 5, 3, 0, Math.PI * 2); c.fill()
    c.fillStyle = '#f0f1f4'
    c.beginPath(); c.arc(-15, 5, 1.2, 0, Math.PI * 2); c.fill()
    c.beginPath(); c.arc(13, 5, 1.2, 0, Math.PI * 2); c.fill()
    c.restore()
  }
  function roundRect(c: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
    c.beginPath()
    c.moveTo(x + r, y)
    c.arcTo(x + w, y, x + w, y + h, r)
    c.arcTo(x + w, y + h, x, y + h, r)
    c.arcTo(x, y + h, x, y, r)
    c.arcTo(x, y, x + w, y, r)
    c.closePath()
  }
  tick()
}

function teardownNet() {
  if (netRaf) cancelAnimationFrame(netRaf)
  netRaf = 0
}

onMounted(initNet)
onBeforeUnmount(teardownNet)

const phone = ref('')
const password = ref('')
const loading = ref(false)
const showPwd = ref(false)
const remember = ref(false)

const roleTabs = [
  { key: 'USER' as Role, label: '用户端', desc: '下单 / 支付 / 查轨迹', hint: '用户端 · 下单、支付与物流轨迹查询' },
  { key: 'STAFF' as Role, label: '员工端', desc: '接单 / 路线 / 动态', hint: '员工端 · 接单报价、路线规划、在途动态发布' },
  { key: 'ADMIN' as Role, label: '管理端', desc: '全局 / 统计 / 风控', hint: '管理端 · 订单总览、需求审核与运营统计' },
]

const role = ref<Role>('USER')
const roleIndex = computed(() => roleTabs.findIndex((t) => t.key === role.value))
const roleLabel = computed(() => roleTabs.find((t) => t.key === role.value)?.label ?? '用户端')
const roleHint = computed(() => roleTabs.find((t) => t.key === role.value)?.hint ?? '')

function gotoRegister() {
  router.push({ path: '/user/register', query: { role: role.value } })
}

async function onSubmit() {
  if (!phone.value || !password.value) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    const resp = await auth.login(phone.value, password.value, role.value)
    ElMessage.success(`登录成功，当前角色：${roleLabel.value}`)
    const redirect = route.query.redirect as string | undefined
    if (redirect) {
      router.push(redirect)
      return
    }
    // 若实际角色与所选角色不符，仍按实际角色进入对应首页
    const home = resp.role === 'STAFF' ? '/staff' : resp.role === 'ADMIN' ? '/admin' : '/user'
    router.push(home)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '登录失败'
    if (msg.includes('role mismatch') || msg.includes('ROLE_FORBIDDEN') || msg.toLowerCase().includes('role')) {
      ElMessage.error(`账号不具备${roleLabel.value}权限，请使用对应角色账号登录`)
    } else {
      ElMessage.error(msg)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  position: relative;
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  overflow: hidden;
}

/* ---------- 背景层 ---------- */
.login-bg__city-map {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.9;
  background:
    /* 斜切街区（大网格，斜 12°） */
    repeating-linear-gradient(
      78deg,
      rgba(30, 58, 95, 0.05) 0 1px,
      transparent 1px 90px
    ),
    /* 斜切街区（横向） */
    repeating-linear-gradient(
      -12deg,
      rgba(30, 58, 95, 0.04) 0 1px,
      transparent 1px 90px
    ),
    /* 城市道路（细网格，斜 12°，30px 间距） */
    repeating-linear-gradient(
      78deg,
      rgba(30, 58, 95, 0.06) 0 0.5px,
      transparent 0.5px 30px
    ),
    repeating-linear-gradient(
      -12deg,
      rgba(30, 58, 95, 0.05) 0 0.5px,
      transparent 0.5px 30px
    ),
    /* 底部青绿晕染（呼应参考图下缘） */
    radial-gradient(ellipse 120% 40% at 50% 118%,
      rgba(158, 214, 205, 0.30) 0%,
      rgba(178, 222, 216, 0.14) 45%,
      transparent 72%),
    /* 中央卡片区浅白雾 */
    radial-gradient(ellipse 55% 42% at 50% 46%,
      rgba(255, 255, 255, 0.75) 0%,
      rgba(248, 250, 252, 0.4) 55%,
      transparent 82%),
    /* 整体底色 */
    linear-gradient(180deg, #eef1f5 0%, #f4f6f8 55%, #e9eef2 100%);
  background-blend-mode: normal, normal, normal, normal, normal, normal, normal;
}
.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
/* 参考图风格：深蓝定位 pin + 青色发光环 */
.login-bg__marker {
  position: absolute;
  width: 10px;
  height: 10px;
  transform: translate(-50%, -50%);
  z-index: 2;
  pointer-events: none;
}
.login-bg__marker::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: #1e3a5f;
  box-shadow: 0 1px 2px rgba(30, 58, 95, 0.35);
}
.login-bg__marker::after {
  content: "";
  position: absolute;
  inset: -14px;
  border-radius: 50%;
  background: radial-gradient(circle,
    rgba(122, 210, 220, 0.55) 0%,
    rgba(122, 210, 220, 0.22) 42%,
    transparent 72%);
  animation: marker-pulse 3.2s ease-in-out infinite;
}
.login-bg__marker:last-child::after {
  animation-delay: -1.6s;
}
@keyframes marker-pulse {
  0%, 100% { opacity: 0.45; transform: scale(0.88); }
  50% { opacity: 1; transform: scale(1.12); }
}
.login-bg__vignette {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 55% 50% at 50% 45%,
      rgba(255, 255, 255, 0.62) 0%,
      rgba(246, 247, 249, 0.35) 48%,
      rgba(230, 232, 236, 0.12) 78%,
      rgba(226, 228, 232, 0.05) 100%);
  pointer-events: none;
}
.login-bg__pins {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.pin {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(79, 98, 117, 0.35);
  box-shadow: 0 0 0 0 rgba(79, 98, 117, 0.35);
  opacity: 0;
  animation: pin-blink 10s ease-in-out infinite;
}
@keyframes pin-blink {
  0%, 82%, 100% { opacity: 0; transform: scale(0.6); box-shadow: 0 0 0 0 rgba(79,98,117,0.3); }
  86% { opacity: 0.8; transform: scale(1); box-shadow: 0 0 0 6px rgba(79,98,117,0.12); }
  92% { opacity: 0.5; transform: scale(1); box-shadow: 0 0 0 12px rgba(79,98,117,0.05); }
}
.login-bg__roads {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 1;
  pointer-events: none;
}
.login-bg__region {
  mask-image: radial-gradient(ellipse 85% 80% at 50% 50%, #000 25%, transparent 88%);
  -webkit-mask-image: radial-gradient(ellipse 85% 80% at 50% 50%, #000 25%, transparent 88%);
}
.login-bg__trunk,
.login-bg__branch,
.login-bg__nodes {
  mask-image: radial-gradient(ellipse 80% 70% at 50% 50%, #000 30%, transparent 85%);
  -webkit-mask-image: radial-gradient(ellipse 80% 70% at 50% 50%, #000 30%, transparent 85%);
}
.login-bg__ruler,
.login-bg__coords,
.login-bg__scale {
  mask-image: radial-gradient(ellipse 96% 92% at 50% 50%, #000 45%, transparent 92%);
  -webkit-mask-image: radial-gradient(ellipse 96% 92% at 50% 50%, #000 45%, transparent 92%);
}
.login-bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(30, 58, 95, 0.04) 0.5px, transparent 0.5px),
    linear-gradient(90deg, rgba(30, 58, 95, 0.04) 0.5px, transparent 0.5px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 92% 75% at 50% 45%, #000 32%, transparent 78%);
  -webkit-mask-image: radial-gradient(ellipse 92% 75% at 50% 45%, #000 32%, transparent 78%);
}
.login-bg__net {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.9;
  pointer-events: none;
}
.login-bg__net--over {
  z-index: 5;
}
.login-bg__scan {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(79,98,117,0.05) 1px, transparent 1px);
  background-size: 100% 120px;
  opacity: 0.6;
  animation: scan-drift 40s linear infinite;
  pointer-events: none;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 50%, #000 30%, transparent 85%);
  -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 50%, #000 30%, transparent 85%);
}
@keyframes scan-drift {
  from { background-position: 0 0; }
  to { background-position: 0 240px; }
}
.login-bg__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.55;
}
.login-bg__glow--a {
  width: 480px; height: 480px;
  left: 6%; top: 8%;
  background: rgba(150, 158, 172, 0.22);
  animation: glow-drift-a 26s ease-in-out infinite alternate;
}
.login-bg__glow--b {
  width: 420px; height: 420px;
  right: 4%; bottom: 6%;
  background: rgba(150, 155, 165, 0.16);
  animation: glow-drift-b 32s ease-in-out infinite alternate;
}
@keyframes glow-drift-a {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(46px, 30px) scale(1.12); }
}
@keyframes glow-drift-b {
  from { transform: translate(0, 0) scale(1.05); }
  to { transform: translate(-38px, -24px) scale(0.94); }
}

/* ---------- 进入入口 ---------- */
.login-shell {
  position: relative;
  z-index: 4;
  width: 100%;
  max-width: 880px;
  min-height: 600px;
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 0;
  background: linear-gradient(180deg, rgba(252, 253, 254, 0.92), rgba(247, 249, 251, 0.90));
  border: 1px solid var(--glass-border);
  border-radius: 18px;
  overflow: hidden;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 12px 32px rgba(15, 23, 42, 0.08),
    0 28px 64px rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition:
    opacity 0.5s var(--ease),
    transform 0.5s var(--ease);
}
.login-shell--hidden {
  opacity: 0;
  transform: scale(0.96) translateY(8px);
  pointer-events: none;
  visibility: hidden;
}

/* ---------- 左：品牌区 ---------- */
.login-brand {
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  position: relative;
  background:
    radial-gradient(420px 320px at 20% 0%, rgba(150, 158, 172, 0.18), transparent 65%);
}
.login-brand::after {
  content: "";
  position: absolute;
  top: 12%;
  bottom: 12%;
  right: -1px;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent,
    var(--glass-border-2) 22%,
    var(--glass-border-2) 78%,
    transparent
  );
}
.login-brand__logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--primary);
  margin-bottom: 28px;
}
.login-brand__name {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: var(--text-hi);
}
.login-brand__title {
  margin: 0 0 10px;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 0.01em;
}
.login-brand__sub {
  margin: 0 0 32px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-mid);
}

.login-brand__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin-bottom: 8px;
  border-top: 1px solid var(--glass-border);
  padding-top: 14px;
}

.metric {
  position: relative;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  border-left: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.45);
  border-radius: var(--radius-sm);
}
.metric:first-child { border-left: none; }
.metric__icon {
  color: var(--primary);
  display: inline-flex;
}
.metric__val {
  font-size: 19px;
  font-weight: 700;
  color: var(--text-hi);
  letter-spacing: -0.01em;
}
.metric__val small {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-low);
  margin-left: 2px;
}
.metric__label {
  font-size: 10px;
  color: var(--text-low);
  letter-spacing: 0.05em;
}

.login-brand__foot { opacity: 0.6; }

.login-brand__metric-note {
  margin-top: 4px;
  font-size: 10px;
  color: var(--text-low);
  letter-spacing: 0.06em;
}

.login-brand__foot {
  margin-top: auto;
  font-size: 11px;
  color: var(--text-low);
  letter-spacing: 0.08em;
}

/* ---------- 右：登录面板 ---------- */
.login-panel {
  padding: 44px 36px 36px;
  display: flex;
  flex-direction: column;
}

/* 角色切换 tabs */
.role-tabs {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: var(--glass-bg-2);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 4px;
  margin-bottom: 18px;
  overflow: hidden;
}
.role-tab {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 0 8px;
  background: none;
  border: none;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text-mid);
  cursor: pointer;
  transition: color 0.2s var(--ease);
  font-family: inherit;
}
.role-tab:hover { color: var(--text-hi); }
.role-tab--active { color: var(--text-hi); }
.role-tab__icon { opacity: 0.75; }
.role-tab--active .role-tab__icon { opacity: 1; color: var(--primary); }
.role-tabs__indicator {
  position: absolute;
  top: 4px; left: 4px;
  width: calc((100% - 8px) / 3);
  height: calc(100% - 8px);
  background: linear-gradient(180deg, rgba(79, 98, 117, 0.12), rgba(79, 98, 117, 0.06));
  border: 1px solid rgba(79, 98, 117, 0.28);
  border-radius: 8px;
  transition: transform 0.28s var(--ease);
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.6), inset 0 -1px 1px rgba(79, 98, 117, 0.12), 0 3px 10px rgba(79, 98, 117, 0.14);
}

.login-panel__hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-mid);
  letter-spacing: 0.05em;
  padding-bottom: 22px;
  border-bottom: 1px solid rgba(79, 98, 117, 0.10);
  margin-bottom: 24px;
}

.login-panel__form {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.login-panel__form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.login-panel__form :deep(.el-form-item__label) {
  font-size: 12.5px;
  color: var(--text-mid);
}

/* 固定上方标签输入 */
.field {
  width: 100%;
  min-width: 0;
  margin-bottom: 18px;
  position: relative;
}
.field__label {
  display: block;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text-mid);
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}
.field__input {
  display: block;
  width: 100%;
  box-sizing: border-box;
  height: 46px;
  padding: 0 14px;
  border: 1px solid rgba(79, 98, 117, 0.22);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  color: var(--text-hi);
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s var(--ease), box-shadow 0.2s var(--ease), background 0.2s var(--ease);
}
.field__input::placeholder { color: var(--text-low); }
.field__input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 98, 117, 0.14);
  background: #fff;
}
.field--with-eye .field__input { padding-right: 42px; }
.field__eye {
  position: absolute;
  right: 12px;
  top: 46px;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-low);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex: none;
  transition: color 0.15s var(--ease);
}
.field--with-eye {
  position: relative;
  min-width: 0;
}
.field__eye:hover { color: var(--primary); }

/* 记住 / 忘记密码 */
.login-panel__extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 4px;
  font-size: 12px;
  color: var(--text-mid);
}
.login-panel__remember {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}
.login-panel__remember-box {
  width: 14px;
  height: 14px;
  accent-color: var(--primary);
  cursor: pointer;
}
.login-panel__forgot {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}
.login-panel__forgot:hover { text-decoration: underline; }

.login-panel__submit {
  width: 100%;
  margin-top: 14px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14.5px;
  letter-spacing: 0.04em;
  font-weight: 700;
  border: 1px solid #1e3a5f;
  border-radius: 12px;
  background: linear-gradient(180deg, #2c5182 0%, #1e3a5f 60%, #16304f 100%);
  box-shadow: 0 2px 8px rgba(30, 58, 95, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.14);
  transition: background 0.18s var(--ease), transform 0.15s var(--ease), box-shadow 0.18s var(--ease);
}
.login-panel__submit em { font-style: normal; }
.login-panel__submit-arrow { transition: transform 0.18s var(--ease); }
.login-panel__submit:hover {
  background: linear-gradient(180deg, #345e93 0%, #2c5182 60%, #1e3a5f 100%);
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(56, 72, 90, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.22);
}
.login-panel__submit:hover .login-panel__submit-arrow { transform: translateX(3px); }
.login-panel__submit:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(56, 72, 90, 0.16), inset 0 2px 4px rgba(0, 0, 0, 0.12);
}
.login-panel__submit:focus-visible {
  outline: 2px solid var(--accent-bright);
  outline-offset: 2px;
}

.login-panel__foot {
  margin-top: 18px;
  text-align: center;
}
.login-panel__reg-btn {
  margin-top: 18px;
  width: 100%;
  padding: 8px;
  font-size: 12px;
  color: var(--text-low);
  background: none;
  border: none;
  cursor: pointer;
  letter-spacing: 0.04em;
  transition: color 0.18s var(--ease);
}
.login-panel__reg-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(79, 98, 117, 0.05);
}
.login-panel__reg-btn:focus-visible {
  outline: 2px solid var(--accent-bright);
  outline-offset: 2px;
}

/* 路线母题：虚线缓动，克制 */
.login-brand__route {
  position: absolute;
  right: 28px;
  top: 26px;
  width: 200px;
  height: 84px;
  opacity: 0.85;
  pointer-events: none;
}
.login-brand__route-line {
  animation: route-dash 9s linear infinite;
}
@keyframes route-dash {
  to { stroke-dashoffset: -260; }
}

/* 控制台边框：错位贴合卡片，像系统入口 */
.login-console {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 920px;
  height: 600px;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.login-console__tl {
  position: absolute;
  left: -22px;
  top: -18px;
  width: 74px;
  height: 54px;
  border-left: 0.5px solid rgba(30, 58, 95, 0.28);
  border-top: 0.5px solid rgba(30, 58, 95, 0.28);
}
.login-console__br {
  position: absolute;
  right: -22px;
  bottom: -18px;
  width: 74px;
  height: 54px;
  border-right: 0.5px solid rgba(30, 58, 95, 0.28);
  border-bottom: 0.5px solid rgba(30, 58, 95, 0.28);
}
.login-console__tick--h {
  position: absolute;
  left: -40px;
  top: 50%;
  width: 34px;
  height: 0.5px;
  background: rgba(30, 58, 95, 0.22);
}
@media (max-width: 1080px) {
  .login-console { display: none; }
}
.login-shell { animation: fade-up 0.4s var(--ease); }

/* ---------- 响应式 ---------- */
@media (max-width: 860px) {
  .login-shell { grid-template-columns: 1fr; max-width: 440px; }
  .login-brand { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .login-brand__route-line { animation: none; }
  .login-brand__route-dot { display: none; }
  .login-shell { animation: none; }
}

@media (prefers-reduced-motion: reduce) {
  .role-tabs__indicator { transition: none; }
  .login-panel__submit { transition: none; }
  .login-panel__reg-btn { transition: none; }
  .login-bg__trunk--flow {
  stroke-dasharray: 4 14;
  animation: trunk-flow 30s linear infinite;
}
@keyframes trunk-flow {
  to { stroke-dashoffset: -180; }
}
.login-bg__scan { animation: none; opacity: 0; }
.login-bg__marker::after { animation: none; opacity: 0.6; }
.login-bg__trunk--flow { animation: none; }
.login-bg__city-rings circle { animation: none; opacity: 0.6; }
.login-bg__radar-sweep { animation: none; }
.login-bg__focus { transition: none; }
  .login-bg__glow--a, .login-bg__glow--b { animation: none; }
  .pin { animation: none; opacity: 0.4; }
}
</style>