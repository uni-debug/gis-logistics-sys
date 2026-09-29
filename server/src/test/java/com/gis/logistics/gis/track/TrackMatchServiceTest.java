package com.gis.logistics.gis.track;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.logistics.LogisticsEventRepository;
import com.gis.logistics.gis.osrm.OsrmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackMatchServiceTest {

    @Mock
    private OsrmClient osrmClient;
    @Mock
    private LogisticsEventRepository eventRepository;

    private TrackMatchService service;

    @BeforeEach
    void setUp() {
        service = new TrackMatchService(osrmClient, eventRepository);
    }

    @Test
    void trackReturnsSnappedLineAndSavesEvents() throws Exception {
        String matchJson = "{\"code\":\"Ok\",\"matchings\":[{\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[116.4,39.9],[116.45,39.95]]}}]}";
        when(osrmClient.matchTrack(any(double[].class)))
                .thenAnswer(inv -> new ObjectMapper().readTree(matchJson));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TrackMatchService.MatchResult result = service.track(1L,
                List.of(new TrackMatchService.TrackPoint(116.4, 39.9),
                        new TrackMatchService.TrackPoint(116.45, 39.95)),
                9L);

        assertEquals(2, result.pointCount());
        assertEquals("LineString", result.snappedLine().getGeometryType());
        verify(eventRepository, times(2)).save(any());
    }

    @Test
    void insufficientPointsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.track(1L, List.of(new TrackMatchService.TrackPoint(116.4, 39.9)), 9L));
        assertEquals(ErrorCode.TRACK_MATCH_FAILED, ex.getCode());
    }

    @Test
    void emptyPointsRejected() {
        assertThrows(BizException.class, () -> service.track(1L, List.of(), 9L));
    }
}
