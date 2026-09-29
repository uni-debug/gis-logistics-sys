<template>
  <div ref="mapEl" class="gis-map"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import maplibregl from 'maplibre-gl'

interface GeoLine {
  type: string
  coordinates: number[][]
}

const props = defineProps<{
  center?: [number, number]
  zoom?: number
  lines?: GeoLine[]
  points?: { lon: number; lat: number; label?: string }[]
}>()

const mapEl = ref<HTMLDivElement | null>(null)
let map: maplibregl.Map | null = null
const DEFAULT_CENTER: [number, number] = [116.4074, 39.9042]
const DEFAULT_ZOOM = 6

function initMap() {
  if (!mapEl.value) return
  map = new maplibregl.Map({
    container: mapEl.value,
    style: 'https://demotiles.maplibre.org/style.json',
    center: props.center ?? DEFAULT_CENTER,
    zoom: props.zoom ?? DEFAULT_ZOOM,
  })
  map.addControl(new maplibregl.NavigationControl())
  map.addControl(new maplibregl.ScaleControl())
  map.on('load', renderLayers)
}

function renderLayers() {
  if (!map) return
  map.getSource('gis-lines') ? undefined : map.addSource('gis-lines', { type: 'geojson', data: lineFeature() })
  map.getSource('gis-points') ? undefined : map.addSource('gis-points', { type: 'geojson', data: pointFeature() })
  map.addLayer({ id: 'gis-lines-layer', type: 'line', source: 'gis-lines', paint: { 'line-color': '#2563eb', 'line-width': 4 } })
  map.addLayer({ id: 'gis-points-layer', type: 'circle', source: 'gis-points', paint: { 'circle-radius': 6, 'circle-color': '#dc2626' } })
  if (props.lines && props.lines.length > 0) {
    fitToLines()
  }
}

function lineFeature() {
  const feats = (props.lines ?? []).map((l) => ({ type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: l.coordinates } }))
  return { type: 'FeatureCollection', features: feats }
}

function pointFeature() {
  const feats = (props.points ?? []).map((p) => ({ type: 'Feature', properties: { label: p.label ?? '' }, geometry: { type: 'Point', coordinates: [p.lon, p.lat] } }))
  return { type: 'FeatureCollection', features: feats }
}

function fitToLines() {
  if (!map || !props.lines || props.lines.length === 0) return
  const coords = props.lines.flatMap((l) => l.coordinates)
  if (coords.length === 0) return
  const bounds = new maplibregl.LngLatBounds()
  for (const c of coords) bounds.extend([c[0], c[1]] as [number, number])
  map.fitBounds(bounds, { padding: 60, duration: 400 })
}

watch(() => [props.lines, props.points], () => {
  if (!map) return
  map.getSource('gis-lines')?.setData(lineFeature() as GeoJSON.GeoJSON)
  map.getSource('gis-points')?.setData(pointFeature() as GeoJSON.GeoJSON)
  if (props.lines && props.lines.length > 0) fitToLines()
}, { deep: true })

onMounted(initMap)
onBeforeUnmount(() => {
  map?.remove()
  map = null
})
</script>

<style scoped>
.gis-map {
  width: 100%;
  height: 480px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
