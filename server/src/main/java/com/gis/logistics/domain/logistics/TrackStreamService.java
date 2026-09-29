package com.gis.logistics.domain.logistics;

import com.gis.logistics.common.log.MdcUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 轨迹实时推送：按订单维护 SSE 订阅者，事件发布时向订阅者广播（关键路径，需人工复核）。
 * 使用进程内广播；跨实例部署需替换为消息中间件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackStreamService {

    private final LogisticsEventRepository eventRepository;

    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 300_000L;

    public SseEmitter subscribe(Long orderId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        subscribers.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(orderId, emitter));
        emitter.onTimeout(() -> remove(orderId, emitter));
        emitter.onError(e -> remove(orderId, emitter));
        // 先推送当前全量快照，保证新订阅者拿到已有轨迹
        pushSnapshot(orderId, emitter);
        return emitter;
    }

    public void broadcast(Long orderId, LogisticsEvent event) {
        List<SseEmitter> list = subscribers.get(orderId);
        if (list == null || list.isEmpty()) {
            return;
        }
        WktPayload payload = new WktPayload(event.getId(), event.getType().name(),
                toWkt(event.getPoint()), event.getOccurredAt().toString());
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.getId()))
                        .name(event.getType().name())
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                remove(orderId, emitter);
                log.warn("sse send failed orderId={} traceId={}", orderId, MdcUtils.getTraceId(), e);
            }
        }
    }

    private void pushSnapshot(Long orderId, SseEmitter emitter) {
        List<WktPayload> snapshot = eventRepository.findByOrderIdOrderByOccurredAtAsc(orderId).stream()
                .map(e -> new WktPayload(e.getId(), e.getType().name(), toWkt(e.getPoint()), e.getOccurredAt().toString()))
                .toList();
        try {
            emitter.send(SseEmitter.event().name("SNAPSHOT").data(snapshot));
        } catch (IOException e) {
            log.warn("sse snapshot failed orderId={}", orderId, e);
        }
    }

    private String toWkt(org.locationtech.jts.geom.Geometry g) {
        return g == null ? null : g.toText();
    }

    public record WktPayload(Long id, String type, String pointWkt, String occurredAt) {}

    private void remove(Long orderId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(orderId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                subscribers.remove(orderId, list);
            }
        }
        emitter.complete();
    }
}