package com.gis.logistics.gis.route;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.gis.osrm.OsrmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutePlanServiceTest {

    @Mock
    private OsrmClient osrmClient;
    @Mock
    private PolylineRouteEngine polylineEngine;
    @Mock
    private RouteTaskRepository routeTaskRepository;

    private RoutePlanService service;

    @BeforeEach
    void setUp() {
        service = new RoutePlanService(osrmClient, polylineEngine, routeTaskRepository, "osrm");
    }

    @Test
    void planPersistsRouteTask() {
        when(osrmClient.planRoute(any(), anyList(), any()))
                .thenReturn(new OsrmClient.Record(8000.0, 120.0,
                        "{\"type\":\"LineString\",\"coordinates\":[[116.4,39.9],[116.45,39.92]]}", 0));
        when(routeTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RouteTask task = service.plan(1L, RouteStrategy.FASTEST, 116.4, 39.9, 116.45, 39.92, null, null);

        assertNotNull(task.getGeometry());
        assertEquals("LineString", task.getGeometry().getGeometryType());
        assertEquals(8000.0, task.getDistanceM());
        assertEquals(120, task.getEtaS());
        verify(routeTaskRepository).save(any());
    }

    @Test
    void multiLineStringHandled() {
        when(osrmClient.planRoute(any(), anyList(), any()))
                .thenReturn(new OsrmClient.Record(5000.0, 90.0,
                        "{\"type\":\"MultiLineString\",\"coordinates\":[[[116.4,39.9],[116.42,39.91]],[[116.45,39.93],[116.47,39.95]]]}", 0));
        when(routeTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RouteTask task = service.plan(1L, RouteStrategy.CHEAPEST, 116.4, 39.9, 116.47, 39.95,
                List.of(), List.of("a", "b"));

        assertEquals("MultiLineString", task.getGeometry().getGeometryType());
        assertEquals("a,b", task.getAvoidSegments());
    }

    @Test
    void invalidCoordinateRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.plan(1L, RouteStrategy.FASTEST, Double.NaN, 39.9, 116.4, 39.9, null, null));
        assertEquals(ErrorCode.POINT_INVALID, ex.getCode());
    }

    @Test
    void badGeometryTypeRejected() {
        when(osrmClient.planRoute(any(), anyList(), any()))
                .thenReturn(new OsrmClient.Record(1.0, 1.0, "{\"type\":\"Point\",\"coordinates\":[1,2]}", 0));
        BizException ex = assertThrows(BizException.class,
                () -> service.plan(1L, RouteStrategy.FASTEST, 116.4, 39.9, 116.4, 39.9, null, null));
        assertEquals(ErrorCode.ROUTE_PLAN_FAILED, ex.getCode());
    }

    @Test
    void polylineEngineUsedWhenConfigured() {
        when(polylineEngine.planLine(any(), anyList(), any()))
                .thenReturn(new OsrmClient.Record(9000.0, 200.0,
                        "{\"type\":\"LineString\",\"coordinates\":[[116.4,39.9],[121.4,31.2]]}", 500));
        when(routeTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoutePlanService lineService = new RoutePlanService(osrmClient, polylineEngine, routeTaskRepository, "line");
        RouteTask task = lineService.plan(1L, RouteStrategy.CHEAPEST, 116.4, 39.9, 121.4, 31.2, null, null);

        assertEquals("LineString", task.getGeometry().getGeometryType());
        assertEquals(9000.0, task.getDistanceM());
        assertEquals(500, task.getCost());
        verify(polylineEngine).planLine(any(), anyList(), any());
        verifyNoInteractions(osrmClient);
    }

    @Test
    void haversineDistanceSanity() {
        // 北京->上海 直线距离约 1075km，Haversine 应在 1.05e6 米量级
        double d = PolylineRouteEngine.haversineMeters(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(d > 1_000_000 && d < 1_150_000, "distance out of range: " + d);
    }
}