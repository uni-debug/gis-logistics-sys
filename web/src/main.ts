import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "maplibre-gl/dist/maplibre-gl.css";
import "./styles/tokens.css";
import App from "./App.vue";
import { router } from "./router";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus);
try {
  app.mount('#app')
  const probe = document.getElementById('boot-probe')
  if (probe?.parentNode) probe.parentNode.removeChild(probe)
  clearTimeout((window as unknown as { __bootProbeTimer?: number }).__bootProbeTimer)
} catch (e) {
  console.error('[SPA mount failed]', e)
  const el = document.getElementById('app')
  if (el) {
    el.innerHTML = '<div style="min-height:60vh;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-family:monospace;font-size:13px;">应用加载失败，请刷新页面（Ctrl+F5）</div>'
  }
}

