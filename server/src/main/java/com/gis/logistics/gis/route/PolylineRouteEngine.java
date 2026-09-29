package com.gis.logistics.gis.route;

import com.gis.logistics.gis.osrm.OsrmClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 折线路线引擎（GIS 关键路径，需人工复核）。
 * 当 OSRM 引擎不可用时（如网络无法部署 OSRM 服务），用 Haversine 距离 + 固定成本模型
 * 按起终点/途经点坐标生成折线路线，保证前端地图可画线、距离/成本可估算。
 * 真实 OSRM 部署后切回 osrm 引擎即可。
 */
@Slf4j
@Component
public class PolylineRouteEngine {

    /** 默认行驶速度（米/秒），用于估算 duration。 */
    private static final double DEFAULT_SPEED_MS = 22.0;

    @Value("${app.osrm.cost-fen-per-km:200}")
    private int costFenPerKm;

    @Value("${app.osrm.cost-fen-per-avoid-segment:50}")
    private int costFenPerAvoidSegment;

    /** 按 waypoints 顺序生成折线 + Haversine 总距离 + 成本估算。 */
    public OsrmClient.Record planLine(RouteStrategy strategy, List<double[]> waypoints, List<String> avoid) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalStateException("need at least 2 waypoints");
        }
        double totalM = 0.0;
        List<double[]> coords = new java.util.ArrayList<>(waypoints);
        for (int i = 0; i < waypoints.size(); i++) {
            double[] w = waypoints.get(i);
            if (i > 0) {
                double[] prev = waypoints.get(i - 1);
                totalM += haversineMeters(prev[1], prev[0], w[1], w[0]);
            }
        }
        double durationS = totalM / DEFAULT_SPEED_MS;
        int costFen = strategy == RouteStrategy.CHEAPEST
                ? estimateCostFen(totalM, avoid)
                : 0;
        log.debug("polyline engine: waypoints={}, distanceM={}, durationS={}, costFen={}",
                waypoints.size(), totalM, durationS, costFen);
        return new OsrmClient.Record(totalM, durationS, toGeoJsonLine(coords), costFen);
    }

    /** Haversine 球面距离（米）。 */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** 成本估算（分）：基础公里费 + 规避路段惩罚。 */
    private int estimateCostFen(double distanceM, List<String> avoid) {
        double km = distanceM / 1000.0;
        int baseFen = (int) Math.round(km * costFenPerKm);
        int penaltyFen = (avoid == null ? 0 : avoid.size()) * costFenPerAvoidSegment;
        return baseFen + penaltyFen;
    }

    /** 把有序坐标点转成 GeoJSON LineString（OSM 坐标顺序 lon,lat）。 */
    private String toGeoJsonLine(List<double[]> coords) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"LineString\",\"coordinates\":[");
        for (int i = 0; i < coords.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            double[] c = coords.get(i);
            sb.append('[').append(c[0]).append(',').append(c[1]).append(']');
        }
        sb.append("]}");
        return sb.toString();
    }
}
