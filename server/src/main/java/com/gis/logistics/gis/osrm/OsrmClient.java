package com.gis.logistics.gis.osrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.gis.route.RouteStrategy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * OSRM 引擎客户端（GIS 关键路径，需人工复核）。
 * 封装 routing（路线规划）与 match（轨迹吸附）。失败统一抛 ROUTE_OSRM_UNAVAILABLE。
 */
@Slf4j
@Component
public class OsrmClient {

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final int costFenPerKm;
    private final int costFenPerAvoidSegment;

    public OsrmClient(ObjectMapper objectMapper, OkHttpClient httpClient, @Value("${app.osrm.base-url:http://127.0.0.1:5000}") String baseUrl,
                      @Value("${app.osrm.cost-fen-per-km:200}") int costFenPerKm,
                      @Value("${app.osrm.cost-fen-per-avoid-segment:50}") int costFenPerAvoidSegment) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.costFenPerKm = costFenPerKm;
        this.costFenPerAvoidSegment = costFenPerAvoidSegment;
    }



    public Record planRoute(RouteStrategy strategy, List<double[]> waypoints, List<String> avoid) {
        if (waypoints.size() < 2) {
            throw new BizException(ErrorCode.ROUTE_PLAN_FAILED, "need at least 2 waypoints");
        }
        String path = profile(strategy) + "/route/geojson";
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            if (i > 0) {
                coords.append(",");
            }
            coords.append(waypoints.get(i)[0]).append(",").append(waypoints.get(i)[1]);
        }
        String url = baseUrl + "/" + path + "/" + coords;
        JsonNode route = getJson(url).path("routes").get(0);
        if (route == null) {
            throw new BizException(ErrorCode.ROUTE_PLAN_FAILED, "osrm returned no route");
        }
        double distanceM = route.path("distance").asDouble();
        double durationS = route.path("duration").asDouble();
        String geometry = route.path("geometry").asText();
        int cost = strategy == RouteStrategy.CHEAPEST
                ? estimateCostFen(distanceM, avoid)
                : 0;
        return new Record(distanceM, durationS, geometry, cost);
    }

    public JsonNode matchTrack(double[] lonLat) {
        String url = baseUrl + "/" + profile(RouteStrategy.FASTEST) + "/match";
        StringBuilder coords = new StringBuilder();
        for (int i = 0; i < lonLat.length; i += 2) {
            if (i > 0) {
                coords.append(",");
            }
            coords.append(lonLat[i]).append(",").append(lonLat[i + 1]);
        }
        return getJson(url + "/" + coords);
    }

    private String profile(RouteStrategy strategy) {
        return "car";
    }

    private int estimateCostFen(double distanceM, List<String> avoid) {
        double km = distanceM / 1000.0;
        int baseFen = (int) Math.round(km * costFenPerKm);
        int penaltyFen = (avoid == null ? 0 : avoid.size()) * costFenPerAvoidSegment;
        return baseFen + penaltyFen;
    }

    private JsonNode getJson(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("osrm http {} url={}", response.code(), url);
                throw new BizException(ErrorCode.ROUTE_OSRM_UNAVAILABLE, "osrm http " + response.code());
            }
            return objectMapper.readTree(response.body().string());
        } catch (IOException e) {
            log.error("osrm unreachable url={}", url, e);
            throw new BizException(ErrorCode.ROUTE_OSRM_UNAVAILABLE, "osrm unreachable");
        }
    }

    public record Record(double distanceM, double durationS, String geometry, int cost) {}
}
