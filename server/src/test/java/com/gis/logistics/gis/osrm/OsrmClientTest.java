package com.gis.logistics.gis.osrm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.gis.route.RouteStrategy;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OsrmClientTest {

    private MockWebServer server;
    private OsrmClient client;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
        client = new OsrmClient(new ObjectMapper(), new OkHttpClient(), "http://" + server.getHostName() + ":" + server.getPort(), 200, 50);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void planRouteReturnsRecord() {
        String json = "{\"code\":\"Ok\",\"routes\":[{\"distance\":12000.5,\"duration\":300.2,\"geometry\":\"LINESTRING(116.4,39.9,116.5,39.95)\"}]}";
        server.enqueue(new MockResponse().setBody(json));

        OsrmClient.Record rec = client.planRoute(RouteStrategy.FASTEST,
                List.of(new double[]{116.4, 39.9}, new double[]{116.5, 39.95}), List.of());

        assertEquals(12000.5, rec.distanceM(), 0.001);
        assertEquals(300.2, rec.durationS(), 0.001);
        assertEquals(0, rec.cost());
    }

    @Test
    void cheapestStrategyComputesCost() {
        String json = "{\"code\":\"Ok\",\"routes\":[{\"distance\":5000.0,\"duration\":600.0,\"geometry\":\"LINESTRING(116.4,39.9,116.45,39.92)\"}]}";
        server.enqueue(new MockResponse().setBody(json));

        OsrmClient.Record rec = client.planRoute(RouteStrategy.CHEAPEST,
                List.of(new double[]{116.4, 39.9}, new double[]{116.45, 39.92}), List.of("seg1", "seg2"));

        // 5km * 200fen + 2 avoid * 50fen = 1100
        assertEquals(1100, rec.cost());
    }

    @Test
    void costWeightsAreConfigurable() {
        OsrmClient custom = new OsrmClient(new ObjectMapper(), new OkHttpClient(), "http://" + server.getHostName() + ":" + server.getPort(), 100, 10);
        String json = "{\"code\":\"Ok\",\"routes\":[{\"distance\":5000.0,\"duration\":600.0,\"geometry\":\"LINESTRING(116.4,39.9,116.45,39.92)\"}]}";
        server.enqueue(new MockResponse().setBody(json));

        OsrmClient.Record rec = custom.planRoute(RouteStrategy.CHEAPEST,
                List.of(new double[]{116.4, 39.9}, new double[]{116.45, 39.92}), List.of("a", "b", "c"));

        // 5km * 100fen + 3 avoid * 10fen = 530
        assertEquals(530, rec.cost());
    }

    @Test
    void insufficientWaypointsRejected() {
        BizException ex = assertThrows(BizException.class,
                () -> client.planRoute(RouteStrategy.FASTEST, List.of(new double[]{116.4, 39.9}), List.of()));
        assertEquals(ErrorCode.ROUTE_PLAN_FAILED, ex.getCode());
    }

    @Test
    void httpErrorMappedToUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500));
        BizException ex = assertThrows(BizException.class,
                () -> client.planRoute(RouteStrategy.FASTEST,
                        List.of(new double[]{116.4, 39.9}, new double[]{116.5, 39.95}), List.of()));
        assertEquals(ErrorCode.ROUTE_OSRM_UNAVAILABLE, ex.getCode());
    }
}


