import { ref, onBeforeUnmount, onMounted, type Ref } from 'vue'
import type { LogisticsEvent } from '@/types'

interface TrackStreamOptions {
  orderId: Ref<number | null>
  role: 'user' | 'staff'
  onEvent?: (event: LogisticsEvent) => void
  onSnapshot?: (events: LogisticsEvent[]) => void
}

export function useTrackStream(options: TrackStreamOptions) {
  const connected = ref(false)
  let source: EventSource | null = null
  let reconnectTimer: number | null = null
  let reconnectAttempts = 0
  const MAX_RECONNECT = 5

  function open() {
    const id = options.orderId.value
    if (!id) return
    close()
    const base = '/api/v1'
    const token = localStorage.getItem('token')
    const url = `${base}/${options.role}/orders/${id}/track-stream` + (token ? `?token=${encodeURIComponent(token)}` : '')
    source = new EventSource(url)

    source.addEventListener('SNAPSHOT', (e) => {
      try {
        const snapshot: LogisticsEvent[] = JSON.parse((e as MessageEvent).data)
        options.onSnapshot?.(snapshot)
      } catch {
        /* ignore */
      }
    })

    ;(['PICKED', 'IN_TRANSIT', 'ARRIVED_DELIVERY', 'DELIVERED'] as const).forEach((type) => {
      source?.addEventListener(type, (e) => {
        try {
          const raw = JSON.parse((e as MessageEvent).data)
          const event: LogisticsEvent = {
            id: raw.id,
            orderId: options.orderId.value!,
            type: raw.type,
            point: raw.pointWkt ?? null,
            occurredAt: raw.occurredAt,
            operatorId: null,
          }
          options.onEvent?.(event)
        } catch {
          /* ignore */
        }
      })
    })

    source.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
    }

    source.onerror = () => {
      connected.value = false
      source?.close()
      source = null
      if (reconnectAttempts < MAX_RECONNECT) {
        const delay = Math.min(30_000, 1_000 * 2 ** reconnectAttempts)
        reconnectAttempts++
        reconnectTimer = window.setTimeout(open, delay)
      }
    }
  }

  function close() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    source?.close()
    source = null
    connected.value = false
  }

  onMounted(open)
  onBeforeUnmount(close)

  return { connected, open, close }
}
