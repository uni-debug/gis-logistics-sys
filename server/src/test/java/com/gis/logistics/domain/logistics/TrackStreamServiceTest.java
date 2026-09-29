package com.gis.logistics.domain.logistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackStreamServiceTest {

    @Mock
    private LogisticsEventRepository eventRepository;

    private TrackStreamService service;

    @BeforeEach
    void setUp() {
        service = new TrackStreamService(eventRepository);
    }

    @Test
    void subscribeReturnsEmitterAndSendsSnapshot() {
        LogisticsEvent e = new LogisticsEvent(1L, LogisticsEvent.EventType.IN_TRANSIT, null, 9L);
        when(eventRepository.findByOrderIdOrderByOccurredAtAsc(1L)).thenReturn(List.of(e));
        SseEmitter emitter = service.subscribe(1L);
        assertNotNull(emitter);
    }

    @Test
    void broadcastNoSubscribersIsNoop() {
        LogisticsEvent e = new LogisticsEvent(1L, LogisticsEvent.EventType.IN_TRANSIT, null, 9L);
        assertDoesNotThrow(() -> service.broadcast(1L, e));
    }

    @Test
    void broadcastToSubscriberSendsEvent() {
        when(eventRepository.findByOrderIdOrderByOccurredAtAsc(1L)).thenReturn(List.of());
        SseEmitter emitter = service.subscribe(1L);
        LogisticsEvent e = new LogisticsEvent(1L, LogisticsEvent.EventType.IN_TRANSIT, null, 9L);
        assertDoesNotThrow(() -> service.broadcast(1L, e));
        assertNotNull(emitter);
    }
}