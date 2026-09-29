package com.gis.logistics.domain.feedback;

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
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(feedbackRepository);
    }

    @Test
    void submitPersists() {
        when(feedbackRepository.save(any())).thenAnswer(inv -> {
            Feedback f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });
        Feedback f = service.submit(10L, Feedback.Type.COMPLAIN, "投诉内容");
        assertEquals(Feedback.Status.OPEN, f.getStatus());
        assertEquals(1L, f.getId());
    }

    @Test
    void submitRejectsBlank() {
        assertThrows(BizException.class, () -> service.submit(10L, Feedback.Type.COMPLAIN, "  "));
    }

    @Test
    void processChangesStatus() {
        Feedback f = feedback(Feedback.Status.OPEN);
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(f));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Feedback after = service.process(1L, Feedback.Status.PROCESSING);
        assertEquals(Feedback.Status.PROCESSING, after.getStatus());
    }

    @Test
    void replyClosesAndSetsText() {
        Feedback f = feedback(Feedback.Status.OPEN);
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(f));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Feedback after = service.reply(1L, "已处理");
        assertEquals("已处理", after.getReply());
        assertEquals(Feedback.Status.CLOSED, after.getStatus());
    }

    @Test
    void missingThrowsNotFound() {
        when(feedbackRepository.findById(404L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.process(404L, Feedback.Status.PROCESSING));
        assertEquals(ErrorCode.GENERIC_NOT_FOUND, ex.getCode());
    }

    private Feedback feedback(Feedback.Status s) {
        Feedback f = new Feedback(10L, Feedback.Type.COMPLAIN, "c");
        f.setId(1L);
        f.setStatus(s);
        return f;
    }
}
