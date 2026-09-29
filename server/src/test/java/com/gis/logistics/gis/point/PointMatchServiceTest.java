package com.gis.logistics.gis.point;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PointMatchServiceTest {

    private final PointMatchService service = new PointMatchService();

    @Test
    void nearestWithinRadiusReturnsClosest() {
        var candidates = List.of(
                new PointMatchService.Candidate("A", 116.40, 39.90),
                new PointMatchService.Candidate("B", 116.45, 39.95),
                new PointMatchService.Candidate("C", 116.50, 40.00));
        // 目标靠近 B
        var result = service.nearest(candidates, 116.45, 39.95, 2000);
        assertNotNull(result);
        assertEquals("B", result.candidate().name());
        assertTrue(result.distanceM() < 100);
    }

    @Test
    void outsideRadiusReturnsNull() {
        var candidates = List.of(new PointMatchService.Candidate("A", 116.40, 39.90));
        var result = service.nearest(candidates, 116.50, 40.00, 1000);
        assertNull(result);
    }

    @Test
    void emptyCandidatesReturnsNull() {
        assertNull(service.nearest(List.of(), 116.4, 39.9, 5000));
        assertNull(service.nearest(null, 116.4, 39.9, 5000));
    }

    @Test
    void multipleInRadiusPicksMinDistance() {
        var candidates = List.of(
                new PointMatchService.Candidate("far", 116.43, 39.93),
                new PointMatchService.Candidate("near", 116.41, 39.91));
        var result = service.nearest(candidates, 116.41, 39.91, 3000);
        assertEquals("near", result.candidate().name());
    }
}
