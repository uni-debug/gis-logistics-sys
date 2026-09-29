package com.gis.logistics.domain.notice;

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
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    private NoticeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeService(noticeRepository);
    }

    @Test
    void createSavesDraft() {
        when(noticeRepository.save(any())).thenAnswer(inv -> {
            Notice n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });
        Notice n = service.create("公告", "正文");
        assertEquals(Notice.Status.DRAFT.ordinal(), n.getStatus());
        assertEquals(1L, n.getId());
    }

    @Test
    void createRejectsBlankTitle() {
        assertThrows(BizException.class, () -> service.create("  ", "x"));
    }

    @Test
    void publishSetsPublishedStatus() {
        Notice n = notice(Notice.Status.DRAFT);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(n));
        when(noticeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notice after = service.publish(1L);
        assertEquals(Notice.Status.PUBLISHED.ordinal(), after.getStatus());
    }

    @Test
    void publishMissingThrows() {
        when(noticeRepository.findById(404L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> service.publish(404L));
        assertEquals(ErrorCode.GENERIC_NOT_FOUND, ex.getCode());
    }

    private Notice notice(Notice.Status s) {
        Notice n = new Notice();
        n.setId(1L);
        n.setStatus(s.ordinal());
        n.setTitle("t");
        n.setBody("b");
        return n;
    }
}






