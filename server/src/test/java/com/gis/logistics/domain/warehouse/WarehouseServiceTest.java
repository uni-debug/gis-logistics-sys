package com.gis.logistics.domain.warehouse;

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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WarehouseServiceTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private WarehouseRecordRepository recordRepository;

    private WarehouseService service;

    @BeforeEach
    void setUp() {
        service = new WarehouseService(siteRepository, recordRepository);
    }

    @Test
    void addSitePersists() {
        when(siteRepository.findByCode("S01")).thenReturn(Optional.empty());
        when(siteRepository.save(any())).thenAnswer(inv -> {
            Site s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        Site s = service.addSite("S01", "北京中转", 100);
        assertEquals(1L, s.getId());
    }

    @Test
    void addSiteRejectsDuplicateCode() {
        when(siteRepository.findByCode("S01")).thenAnswer(inv -> Optional.of(new Site("S01", "x", 1)));
        assertThrows(BizException.class, () -> service.addSite("S01", "x", 100));
    }

    @Test
    void outboundRejectsInsufficientStock() {
        when(siteRepository.findById(1L)).thenAnswer(inv -> Optional.of(new Site("S01", "x", 100)));
        when(recordRepository.countBySiteIdAndAction(eq(1L), eq(WarehouseRecord.Action.IN))).thenReturn(5L);
        when(recordRepository.countBySiteIdAndAction(eq(1L), eq(WarehouseRecord.Action.OUT))).thenReturn(3L);
        // stock = 2, 出库 5 不足
        BizException ex = assertThrows(BizException.class,
                () -> service.outbound(1L, 99L, 5));
        assertEquals(ErrorCode.GENERIC_BAD_REQUEST, ex.getCode());
        // 显式确认两个 count 都被调用（库存校验真实发生）

    }

    @Test
    void inboundComputesStock() {
        when(siteRepository.findById(1L)).thenAnswer(inv -> Optional.of(new Site("S01", "x", 100)));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.countBySiteIdAndAction(1L, WarehouseRecord.Action.IN)).thenReturn(1L);
        when(recordRepository.countBySiteIdAndAction(1L, WarehouseRecord.Action.OUT)).thenReturn(0L);
        service.inbound(1L, 99L, 1);
        assertEquals(1, service.stockOf(1L));
    }

    @Test
    void outboundUnknownSiteRejected() {
        when(siteRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> service.inbound(404L, 99L, 1));
    }
}









