package com.gis.logistics.domain.im;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImServiceTest {

    @Mock
    private ImSessionRepository sessionRepository;
    @Mock
    private ImMessageRepository messageRepository;

    private ImService service;

    @BeforeEach
    void setUp() {
        service = new ImService(sessionRepository, messageRepository);
    }

    @Test
    void openSessionCreatesIfAbsent() {
        when(sessionRepository.findFirstByUserIdAndStatus(10L, ImSession.Status.ACTIVE)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ImSession.class))).thenAnswer(inv -> {
            ImSession s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        ImSession s = service.openSession(10L);
        assertEquals(1L, s.getId());
        assertEquals(ImSession.Status.ACTIVE, s.getStatus());
    }

    @Test
    void openSessionReusesActive() {
        ImSession existing = new ImSession(10L);
        when(sessionRepository.findFirstByUserIdAndStatus(10L, ImSession.Status.ACTIVE)).thenReturn(Optional.of(existing));
        ImSession s = service.openSession(10L);
        assertSame(existing, s);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void sendUserMessageKeepsStatus() {
        ImSession s = session(1L, ImSession.Status.ACTIVE);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ImMessage m = service.send(1L, ImMessage.SenderRole.USER, "你好");
        assertEquals("你好", m.getContent());
        assertEquals(ImSession.Status.ACTIVE, s.getStatus());
    }

    @Test
    void sendAdminMessageAssigns() {
        ImSession s = session(1L, ImSession.Status.ACTIVE);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.send(1L, ImMessage.SenderRole.ADMIN, "我来处理");
        assertEquals(ImSession.Status.ASSIGNED, s.getStatus());
    }

    @Test
    void sendBlankRejected() {
        ImSession s = session(1L, ImSession.Status.ACTIVE);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(s));
        assertThrows(BizException.class, () -> service.send(1L, ImMessage.SenderRole.USER, "  "));
    }

    @Test
    void missingSessionThrows() {
        when(sessionRepository.findById(404L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.send(404L, ImMessage.SenderRole.USER, "hi"));
        assertEquals(ErrorCode.GENERIC_NOT_FOUND, ex.getCode());
    }

    private ImSession session(Long id, ImSession.Status st) {
        ImSession s = new ImSession(10L);
        s.setId(id);
        s.setStatus(st);
        return s;
    }
}
