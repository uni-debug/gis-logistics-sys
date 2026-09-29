package com.gis.logistics.domain.review;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论管理：用户评价、员工回复、恶意评论软删、好评率统计。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public Review create(Long orderId, Long userId, Long staffId, int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "rating must be 1-5");
        }
        Review r = new Review(orderId, userId, staffId, rating, content);
        return reviewRepository.save(r);
    }

    @Transactional
    public Review reply(Long reviewId, String replyText) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "review not found: " + reviewId));
        r.setStaffReply(replyText);
        return reviewRepository.save(r);
    }

    @Transactional
    public Review delete(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "review not found: " + reviewId));
        r.setDeleted(true);
        return reviewRepository.save(r);
    }

    public record ApprovalRate(long total, long positive, double rate) {}

    public ApprovalRate approvalRate() {
        long total = reviewRepository.countActive();
        long positive = reviewRepository.countPositiveActive();
        double rate = total == 0 ? 0.0 : (double) positive / total;
        return new ApprovalRate(total, positive, rate);
    }
}
