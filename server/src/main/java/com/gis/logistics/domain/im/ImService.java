package com.gis.logistics.domain.im;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 在线客服：建会话、发消息、分配客服、关闭会话、监控活跃会话。
 */
@Service
@RequiredArgsConstructor
public class ImService {

    private final ImSessionRepository sessionRepository;
    private final ImMessageRepository messageRepository;

    @Transactional
    public ImSession openSession(Long userId) {
        return sessionRepository.findFirstByUserIdAndStatus(userId, ImSession.Status.ACTIVE)
                .orElseGet(() -> sessionRepository.save(new ImSession(userId)));
    }

    @Transactional
    public ImMessage send(Long sessionId, ImMessage.SenderRole role, String content) {
        ImSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "session not found: " + sessionId));
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "message content required");
        }
        ImMessage m = new ImMessage(sessionId, role, content.trim());
        m = messageRepository.save(m);
        s.setLastMsgAt(java.time.LocalDateTime.now());
        if (role == ImMessage.SenderRole.ADMIN) {
            s.setStatus(ImSession.Status.ASSIGNED);
        }
        sessionRepository.save(s);
        return m;
    }

    @Transactional
    public ImSession assign(Long sessionId, Long adminId) {
        ImSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "session not found: " + sessionId));
        s.setAdminId(adminId);
        s.setStatus(ImSession.Status.ASSIGNED);
        return sessionRepository.save(s);
    }

    @Transactional
    public ImSession close(Long sessionId) {
        ImSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "session not found: " + sessionId));
        s.setStatus(ImSession.Status.CLOSED);
        return sessionRepository.save(s);
    }

    public List<ImMessage> history(Long sessionId) {
        return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
    }

    public List<ImSession> activeSessions() {
        return sessionRepository.findByStatus(ImSession.Status.ACTIVE, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }
}

