package com.gis.logistics.domain.review;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository);
    }

    @Test
    void createValidRating() {
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        Review r = service.create(1L, 10L, 20L, 5, "good");
        assertEquals(5, r.getRating());
    }

    @Test
    void createRejectsBadRating() {
        assertThrows(BizException.class, () -> service.create(1L, 10L, 20L, 0, "x"));
    }

    @Test
    void deleteSoftRemoves() {
        Review r = review(1L, 5, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(r));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Review after = service.delete(1L);
        assertTrue(after.getDeleted());
    }

    @Test
    void approvalRateComputed() {
        when(reviewRepository.countActive()).thenReturn(10L);
        when(reviewRepository.countPositiveActive()).thenReturn(8L);
        ReviewService.ApprovalRate rate = service.approvalRate();
        assertEquals(0.8, rate.rate(), 0.0001);
    }

    @Test
    void approvalRateEmptyIsZero() {
        when(reviewRepository.countActive()).thenReturn(0L);
        when(reviewRepository.countPositiveActive()).thenReturn(0L);
        assertEquals(0.0, service.approvalRate().rate(), 0.0001);
    }

    private Review review(Long id, int rating, boolean deleted) {
        Review r = new Review(1L, 10L, 20L, rating, "c");
        r.setId(id);
        r.setDeleted(deleted);
        return r;
    }
}
