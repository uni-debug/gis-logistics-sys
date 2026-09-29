package com.gis.logistics.gis.route;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolylineRouteEngineTest {

    @Test
    void planLineReturnsParseableGeoJson() {
        PolylineRouteEngine engine = new PolylineRouteEngine();
        var rec = engine.planLine(RouteStrategy.CHEAPEST,
                List.of(new double[]{116.4, 39.9}, new double[]{121.4, 31.2}), List.of());
        assertNotNull(rec.geometry());
        assertTrue(rec.geometry().contains("\"LineString\""));
        assertTrue(rec.distanceM() > 1_000_000, "beijing-shanghai > 1000km");
        var geom = com.gis.logistics.gis.util.GeometryUtil.parseGeoJsonLine(rec.geometry());
        assertEquals("LineString", geom.getGeometryType());
        assertEquals(2, geom.getNumPoints());
    }

    @Test
    void haversineDistanceSanity() {
        double d = PolylineRouteEngine.haversineMeters(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(d > 1_000_000 && d < 1_150_000, "distance out of range: " + d);
    }

    @Test
    void haversineSymmetry() {
        double d1 = PolylineRouteEngine.haversineMeters(39.9, 116.4, 31.2, 121.4);
        double d2 = PolylineRouteEngine.haversineMeters(31.2, 121.4, 39.9, 116.4);
        assertEquals(d1, d2, 0.001);
    }

    @Test
    void viaPointsAddDistance() {
        PolylineRouteEngine engine = new PolylineRouteEngine();
        var direct = engine.planLine(RouteStrategy.FASTEST,
                List.of(new double[]{121.4, 31.2}, new double[]{116.4, 39.9}), null);
        var viaXuzhou = engine.planLine(RouteStrategy.FASTEST,
                List.of(new double[]{121.4, 31.2}, new double[]{117.185, 34.2599}, new double[]{116.4, 39.9}), null);
        assertTrue(viaXuzhou.distanceM() > direct.distanceM(), "via route longer than direct");
        var geom = com.gis.logistics.gis.util.GeometryUtil.parseGeoJsonLine(viaXuzhou.geometry());
        assertEquals(3, geom.getNumPoints());
    }

    @Test
    void tooFewWaypointsThrows() {
        PolylineRouteEngine engine = new PolylineRouteEngine();
        assertThrows(IllegalStateException.class,
                () -> engine.planLine(RouteStrategy.FASTEST, List.of(new double[]{116.4, 39.9}), null));
    }
}