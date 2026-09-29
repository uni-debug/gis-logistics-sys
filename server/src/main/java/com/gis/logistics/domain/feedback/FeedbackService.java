package com.gis.logistics.domain.feedback;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 留言反馈：提交、分类处理（改状态）、回复、热点问题统计。
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public Feedback submit(Long userId, Feedback.Type type, String content) {
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "feedback content required");
        }
        return feedbackRepository.save(new Feedback(userId, type, content.trim()));
    }

    @Transactional
    public Feedback process(Long id, Feedback.Status to) {
        Feedback f = feedbackRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "feedback not found: " + id));
        f.setStatus(to);
        return feedbackRepository.save(f);
    }

    @Transactional
    public Feedback reply(Long id, String replyText) {
        Feedback f = feedbackRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "feedback not found: " + id));
        f.setReply(replyText);
        f.setStatus(Feedback.Status.CLOSED);
        return feedbackRepository.save(f);
    }

    /** 全量反馈列表（供管理端展示/过滤，避免枚举转换异常）。 */
    public org.springframework.data.domain.Page<Feedback> all(org.springframework.data.domain.Pageable pageable) {
        return feedbackRepository.findAll(pageable);
    }

    public long openCount() {
        return feedbackRepository.countByStatus(Feedback.Status.OPEN);
    }
}

