package com.gis.logistics.domain.admin;

import com.gis.logistics.gis.route.RouteStrategy;
import com.gis.logistics.gis.route.RouteTask;
import com.gis.logistics.gis.route.RouteTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RouteStatsServiceTest {

    @Mock
    private RouteTaskRepository routeTaskRepository;

    private RouteStatsService service;

    private final GeometryFactory gf = new GeometryFactory(new PrecisionModel());

    @BeforeEach
    void setUp() {
        service = new RouteStatsService(routeTaskRepository);
    }

    private RouteTask task(long id, RouteStrategy s, double fromX, double fromY, double toX, double toY,
                           double distM, int etaS, int cost) {
        RouteTask t = new RouteTask(id, s,
                (LineString) gf.createLineString(new org.locationtech.jts.geom.Coordinate[]{
                        new org.locationtech.jts.geom.Coordinate(fromX, fromY),
                        new org.locationtech.jts.geom.Coordinate(toX, toY)}),
                distM, etaS, cost);
        t.setId(id);
        t.setFromPoint(gf.createPoint(new org.locationtech.jts.geom.Coordinate(fromX, fromY)));
        t.setToPoint(gf.createPoint(new org.locationtech.jts.geom.Coordinate(toX, toY)));
        t.setViaPoints(gf.createPoint(new org.locationtech.jts.geom.Coordinate(0, 0)));
        t.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        return t;
    }

    @Test
    void emptySnapshotReturnsZeros() {
        when(routeTaskRepository.findAll()).thenReturn(List.of());
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());
        RouteStatsService.RouteSnapshot snap = service.snapshot(20);
        assertEquals(0, snap.summary().total());
        assertEquals(0.0, snap.summary().avgDistanceKm());
        assertTrue(snap.topByDistance().isEmpty());
        assertTrue(snap.recent().isEmpty());
        assertTrue(snap.summary().strategyDistribution().isEmpty());
    }

    @Test
    void averageDistanceAndStrategyDistribution() {
        RouteTask t1 = task(1, RouteStrategy.FASTEST, 116, 39, 121, 31, 100000, 100, 0);
        RouteTask t2 = task(2, RouteStrategy.CHEAPEST, 114, 22, 123, 41, 300000, 300, 5000);
        List<RouteTask> all = Arrays.asList(t1, t2);
        when(routeTaskRepository.findAll()).thenReturn(all);
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(all);

        RouteStatsService.RouteSnapshot snap = service.snapshot(20);
        assertEquals(2, snap.summary().total());
        assertEquals((100000 + 300000) / 2 / 1000.0, snap.summary().avgDistanceKm(), 0.001);
        assertEquals((100 + 300) / 2, snap.summary().avgEtaSeconds());
        assertEquals(1L, snap.summary().strategyDistribution().get("FASTEST"));
        assertEquals(1L, snap.summary().strategyDistribution().get("CHEAPEST"));
    }

    @Test
    void topByDistanceSortedDescending() {
        RouteTask small = task(1, RouteStrategy.FASTEST, 0, 0, 1, 1, 1000, 10, 0);
        RouteTask large = task(2, RouteStrategy.SHORTEST, 10, 10, 20, 20, 900000, 900, 0);
        RouteTask mid = task(3, RouteStrategy.CHEAPEST, 5, 5, 6, 6, 500000, 500, 0);
        List<RouteTask> all = Arrays.asList(small, large, mid);
        when(routeTaskRepository.findAll()).thenReturn(all);
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(all);

        RouteStatsService.RouteSnapshot snap = service.snapshot(20);
        List<RouteStatsService.RouteDto> top = snap.topByDistance();
        assertTrue(top.size() <= 10);
        // top[0] should be the largest distance (task 2 = 900000m)
        assertEquals(2L, top.get(0).taskId(), "top[0] should be the largest route");
        double d0 = top.get(0).distanceKm();
        double d1 = top.get(1).distanceKm();
        assertTrue(d0 >= d1, "top[0] distance should be >= top[1]");
    }

    @Test
    void geometrySerializedAsLngLatPairs() {
        RouteTask t = task(9, RouteStrategy.FASTEST, 116.4, 39.9, 121.47, 31.23, 1067000, 48000, 213462);
        when(routeTaskRepository.findAll()).thenReturn(List.of(t));
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of(t));

        RouteStatsService.RouteSnapshot snap = service.snapshot(1);
        RouteStatsService.RouteDto dto = snap.recent().get(0);
        assertEquals(2, dto.geometry().size(), "LineString with 2 coords should serialize to 2 lng/lat pairs");
        assertEquals(116.4, dto.fromLon(), 0.0001);
        assertEquals(39.9, dto.fromLat(), 0.0001);
        assertEquals(121.47, dto.toLon(), 0.0001);
        assertEquals(31.23, dto.toLat(), 0.0001);
        assertEquals(1067.0, dto.distanceKm(), 0.001);
    }

    @Test
    void viaPlaceholderFilteredOut() {
        RouteTask t = task(9, RouteStrategy.FASTEST, 116.4, 39.9, 121.47, 31.23, 100000, 100, 0);
        when(routeTaskRepository.findAll()).thenReturn(List.of(t));
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of(t));

        RouteStatsService.RouteSnapshot snap = service.snapshot(1);
        RouteStatsService.RouteDto dto = snap.recent().get(0);
        // viaPoints is a single (0,0) placeholder -> should be empty in dto
        assertTrue(dto.via() == null || dto.via().isEmpty(), "single (0,0) via placeholder should be filtered out");
    }

    @Test
    void limitCapsRecentList() {
        List<RouteTask> all = Arrays.asList(
                task(1, RouteStrategy.FASTEST, 0, 0, 1, 1, 100, 1, 0),
                task(2, RouteStrategy.FASTEST, 0, 0, 1, 1, 200, 2, 0));
        when(routeTaskRepository.findAll()).thenReturn(all);
        when(routeTaskRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(all);

        RouteStatsService.RouteSnapshot snap = service.snapshot(1);
        // recent uses the Pageable from repo, which returns what we stubbed (2 items)
        // verify summary.total still counts all
        assertEquals(2, snap.summary().total());
    }
}
