<template>
  <div class="gis-map-wrap">
    <div ref="mapEl" class="gis-map"></div>
    <div v-if="showLegend" class="gis-legend">
      <div class="gis-legend__row">
        <span class="gis-legend-item" style="background:#0891b2">揽件</span>
        <span class="gis-legend-item" style="background:#d97706">在途</span>
        <span class="gis-legend-item" style="background:#7c3aed">到达</span>
        <span class="gis-legend-item" style="background:#16a34a">签收</span>
      </div>
      <div class="gis-legend__row">
        <span class="gis-legend-item gis-legend-item--live">最新位置</span>
        <span class="gis-legend-item gis-legend-item--move">移动标记</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, computed } from 'vue'
import maplibregl from 'maplibre-gl'
import type { FeatureCollection } from 'geojson'

interface GeoLine {
  type: string
  coordinates: number[][]
}

interface MapPoint {
  lon: number
  lat: number
  label?: string
  current?: boolean
}

const props = defineProps<{
  center?: [number, number]
  zoom?: number
  lines?: GeoLine[]
  points?: MapPoint[]
}>()

const mapEl = ref<HTMLDivElement | null>(null)
let map: maplibregl.Map | null = null
const DEFAULT_CENTER: [number, number] = [116.4074, 39.9042]
const DEFAULT_ZOOM = 6

const showLegend = computed(() => (props.points?.length ?? 0) > 0)

// 国内可达的瓦片底图（高德 raster 瓦片），避免 carto/osm 不可达导致地图空白
const AUTONAVI_TILES = [
  'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
  'https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
  'https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
  'https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
]
const LIGHT_STYLE: maplibregl.StyleSpecification = {
  version: 8,
  sources: {
    'autonavi-tiles': {
      type: 'raster',
      tiles: AUTONAVI_TILES,
      tileSize: 256,
      attribution: '瓦片源 © 高德',
    },
  },
  layers: [
    { id: 'background', type: 'background', paint: { 'background-color': '#e8edf3' } },
    {
      id: 'autonavi-layer',
      type: 'raster',
      source: 'autonavi-tiles',
      paint: { 'raster-opacity': 0.9, 'raster-saturation': 0, 'raster-brightness-max': 1 },
    },
  ],
}

function initMap() {
  if (!mapEl.value) return
  map = new maplibregl.Map({
    container: mapEl.value,
    style: LIGHT_STYLE,
    center: props.center ?? DEFAULT_CENTER,
    zoom: props.zoom ?? DEFAULT_ZOOM,
    attributionControl: false,
  })
  map.addControl(new maplibregl.NavigationControl())
  map.addControl(new maplibregl.ScaleControl())
  map.on('load', renderLayers)
}

function lineFeature(): FeatureCollection {
  const feats = (props.lines ?? []).map((l) => ({
    type: 'Feature' as const,
    properties: {},
    geometry: { type: 'LineString' as const, coordinates: l.coordinates },
  }))
  return { type: 'FeatureCollection' as const, features: feats }
}

const POINT_COLORS: Record<string, string> = {
  PICKED: '#0891b2',
  IN_TRANSIT: '#d97706',
  ARRIVED_DELIVERY: '#7c3aed',
  DELIVERED: '#16a34a',
}
const CURRENT_COLOR = '#ef4444'
const MOVE_COLOR = '#3b82f6'

function pointColor(label: string | undefined, current: boolean | undefined): string {
  if (current) return CURRENT_COLOR
  if (label && POINT_COLORS[label]) return POINT_COLORS[label]
  return '#64748b'
}

function pointFeature(): FeatureCollection {
  const feats = (props.points ?? []).map((p) => ({
    type: 'Feature' as const,
    properties: {
      label: p.label ?? '',
      current: p.current ?? false,
      color: pointColor(p.label, p.current),
    },
    geometry: { type: 'Point' as const, coordinates: [p.lon, p.lat] as [number, number] },
  }))
  return { type: 'FeatureCollection' as const, features: feats }
}

function emptyFeature(): FeatureCollection {
  return { type: 'FeatureCollection' as const, features: [] }
}

function moveFeature(): FeatureCollection {
  const line = props.lines?.[props.lines.length - 1]
  const hasCurrent = (props.points ?? []).some((p) => p.current)
  if (!line || line.coordinates.length < 2 || !hasCurrent) return emptyFeature()
  return {
    type: 'FeatureCollection' as const,
    features: [
      {
        type: 'Feature' as const,
        properties: {},
        geometry: { type: 'Point' as const, coordinates: line.coordinates[0] as [number, number] },
      },
    ],
  }
}

/* ---------- 段表 & 累积距离 ---------- */
interface SegTable {
  cum: number[]
  total: number
  segs: { a: number[]; b: number[]; len: number; cumStart: number }[]
}

let segTable: SegTable | null = null

function buildSegTable() {
  const line = props.lines?.[props.lines.length - 1]
  if (!line || line.coordinates.length < 2) {
    segTable = null
    return
  }
  const cum: number[] = []
  const segs: SegTable['segs'] = []
  let total = 0
  for (let i = 0; i < line.coordinates.length - 1; i++) {
    const a = line.coordinates[i]
    const b = line.coordinates[i + 1]
    const len = approxDist(a, b)
    total += len
    segs.push({ a, b, len, cumStart: total - len })
    cum.push(total)
  }
  segTable = { cum, total, segs }
}

function approxDist(a: number[], b: number[]): number {
  const toRad = Math.PI / 180
  const lat1 = a[1] * toRad
  const lat2 = b[1] * toRad
  const dLat = (b[1] - a[1]) * toRad
  const dLng = (b[0] - a[0]) * toRad * Math.cos((lat1 + lat2) / 2)
  return Math.sqrt(dLat * dLat + dLng * dLng) * 6371
}

function sampleAt(t: number): [number, number] | null {
  if (!segTable || segTable.total <= 0) return null
  const clamped = Math.max(0, Math.min(t, segTable.total))
  for (const s of segTable.segs) {
    if (clamped <= s.cumStart + s.len || s === segTable.segs[segTable.segs.length - 1]) {
      const frac = s.len > 0 ? (clamped - s.cumStart) / s.len : 0
      const f = Math.max(0, Math.min(1, frac))
      const lon = s.a[0] + (s.b[0] - s.a[0]) * f
      const lat = s.a[1] + (s.b[1] - s.a[1]) * f
      return [lon, lat]
    }
  }
  return null
}

/* ---------- 动画时钟 ---------- */
let pulse = 0
let rafId = 0
let clockStart = 0
const SWEEP_SECONDS = 8
const REDUCED =
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

function startPulse() {
  stopPulse()
  buildSegTable()
  clockStart = performance.now()
  if (REDUCED) {
    setMoveMarkerTo(segTable?.total ?? 0)
    if (map) {
      map.setPaintProperty('gis-current-ring-a', 'circle-radius', 30)
      map.setPaintProperty('gis-current-ring-a', 'circle-opacity', 0.25)
      map.setPaintProperty('gis-current-ring-b', 'circle-radius', 18)
      map.setPaintProperty('gis-current-ring-b', 'circle-opacity', 0.35)
    }
    return
  }
  const tick = () => {
    pulse = (pulse + 0.03) % 1
    const ringBPhase = (pulse + 0.5) % 1
    if (map) {
      if (map.getLayer('gis-current-ring-a')) {
        map.setPaintProperty('gis-current-ring-a', 'circle-radius', 10 + pulse * 50)
        map.setPaintProperty('gis-current-ring-a', 'circle-opacity', (1 - pulse) * 0.5)
      }
      if (map.getLayer('gis-current-ring-b')) {
        map.setPaintProperty('gis-current-ring-b', 'circle-radius', 10 + ringBPhase * 50)
        map.setPaintProperty('gis-current-ring-b', 'circle-opacity', (1 - ringBPhase) * 0.4)
      }
    }
    const elapsed = (performance.now() - clockStart) / 1000
    if (segTable && segTable.total > 0) {
      const t = Math.min(elapsed / SWEEP_SECONDS, 1) * segTable.total
      setMoveMarkerTo(t)
    }
    rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)
}

function setMoveMarkerTo(t: number) {
  const pos = sampleAt(t)
  if (!pos || !map) return
  const src = map.getSource('gis-move') as maplibregl.GeoJSONSource | null
  src?.setData({
    type: 'FeatureCollection',
    features: [
      {
        type: 'Feature',
        properties: {},
        geometry: { type: 'Point', coordinates: pos },
      },
    ],
  } as FeatureCollection)
}

function stopPulse() {
  if (rafId) cancelAnimationFrame(rafId)
  rafId = 0
}

function renderLayers() {
  if (!map) return
  if (!map.getSource('gis-lines')) {
    map.addSource('gis-lines', { type: 'geojson', data: lineFeature() })
  }
  if (!map.getSource('gis-points')) {
    map.addSource('gis-points', { type: 'geojson', data: pointFeature() })
  }
  if (!map.getSource('gis-move')) {
    map.addSource('gis-move', { type: 'geojson', data: emptyFeature() })
  }
  if (!map.getLayer('gis-lines-layer')) {
    map.addLayer({
      id: 'gis-lines-layer',
      type: 'line',
      source: 'gis-lines',
      paint: { 'line-color': '#2563eb', 'line-width': 4, 'line-opacity': 0.9 },
    })
  }
  if (!map.getLayer('gis-history-ring')) {
    map.addLayer({
      id: 'gis-history-ring',
      type: 'circle',
      source: 'gis-points',
      filter: ['!', ['get', 'current']],
      paint: {
        'circle-radius': 12,
        'circle-color': MOVE_COLOR,
        'circle-opacity': 0.12,
        'circle-blur': 0.4,
      },
    })
  }
  if (!map.getLayer('gis-points-layer')) {
    map.addLayer({
      id: 'gis-points-layer',
      type: 'circle',
      source: 'gis-points',
      paint: {
        'circle-radius': ['case', ['get', 'current'], 10, 7],
        'circle-color': ['get', 'color'],
        'circle-stroke-width': ['case', ['get', 'current'], 3, 1],
        'circle-stroke-color': '#ffffff',
      },
    })
  }
  if (!map.getLayer('gis-current-ring-b')) {
    map.addLayer({
      id: 'gis-current-ring-b',
      type: 'circle',
      source: 'gis-points',
      filter: ['get', 'current'],
      paint: { 'circle-radius': 10, 'circle-color': CURRENT_COLOR, 'circle-opacity': 0.4 },
    })
  }
  if (!map.getLayer('gis-current-ring-a')) {
    map.addLayer({
      id: 'gis-current-ring-a',
      type: 'circle',
      source: 'gis-points',
      filter: ['get', 'current'],
      paint: { 'circle-radius': 10, 'circle-color': CURRENT_COLOR, 'circle-opacity': 0.5 },
    })
  }
  if (!map.getLayer('gis-current-core')) {
    map.addLayer({
      id: 'gis-current-core',
      type: 'circle',
      source: 'gis-points',
      filter: ['get', 'current'],
      paint: {
        'circle-radius': 9,
        'circle-color': CURRENT_COLOR,
        'circle-stroke-width': 3,
        'circle-stroke-color': '#ffffff',
      },
    })
  }
  if (!map.getLayer('gis-move-marker')) {
    map.addLayer({
      id: 'gis-move-marker',
      type: 'circle',
      source: 'gis-move',
      paint: {
        'circle-radius': 8,
        'circle-color': MOVE_COLOR,
        'circle-stroke-width': 2,
        'circle-stroke-color': '#ffffff',
        'circle-blur': 0.2,
      },
    })
  }
  fitToAll()
  startPulse()
}

function fitToAll() {
  if (!map) return
  const bounds = new maplibregl.LngLatBounds()
  let any = false
  for (const l of props.lines ?? []) {
    for (const c of l.coordinates) {
      bounds.extend([c[0], c[1]] as [number, number])
      any = true
    }
  }
  for (const p of props.points ?? []) {
    bounds.extend([p.lon, p.lat] as [number, number])
    any = true
  }
  if (any) map.fitBounds(bounds, { padding: 60, duration: 400 })
}

watch(
  () => [props.lines, props.points],
  () => {
    if (!map) return
    ;(map.getSource('gis-lines') as maplibregl.GeoJSONSource | null)?.setData(lineFeature())
    ;(map.getSource('gis-points') as maplibregl.GeoJSONSource | null)?.setData(pointFeature())
    ;(map.getSource('gis-move') as maplibregl.GeoJSONSource | null)?.setData(moveFeature())
    fitToAll()
    startPulse()
  },
  { deep: true },
)

onMounted(initMap)
onBeforeUnmount(() => {
  stopPulse()
  map?.remove()
  map = null
})
</script>

<style scoped>
.gis-map-wrap {
  position: relative;
}
.gis-map {
  width: 100%;
  height: 480px;
  border-radius: 12px;
  overflow: hidden;
}
.gis-legend {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--glass-bg-2);
  border: 1px solid var(--glass-border);
  padding: 10px 12px;
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.4;
  z-index: 2;
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
}
.gis-legend__row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.gis-legend-item {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  color: #fff;
  font-weight: 500;
  font-size: 11px;
  letter-spacing: 0.04em;
}
.gis-legend-item--live {
  background: #ef4444;
}
.gis-legend-item--move {
  background: #3b82f6;
}
</style>
