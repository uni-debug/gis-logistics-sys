package com.gis.logistics.gis.route;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.gis.osrm.OsrmClient;
import com.gis.logistics.gis.util.GeometryUtil;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 路线规划服务（GIS 关键路径，需人工复核）。
 * 按 app.osrm.engine 配置切换折线引擎（默认）或 OSRM 引擎；
 * 结果统一经 GeometryUtil 转 JTS Geometry 落 route_tasks。
 */
@Slf4j
@Service
public class RoutePlanService {

    private final OsrmClient osrmClient;
    private final PolylineRouteEngine polylineEngine;
    private final RouteTaskRepository routeTaskRepository;
    private final String engine;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel());

    public RoutePlanService(OsrmClient osrmClient,
                            PolylineRouteEngine polylineEngine,
                            RouteTaskRepository routeTaskRepository,
                            @Value("${app.osrm.engine:line}") String engine) {
        this.osrmClient = osrmClient;
        this.polylineEngine = polylineEngine;
        this.routeTaskRepository = routeTaskRepository;
        this.engine = engine == null || engine.isBlank() ? "line" : engine.toLowerCase();
        log.info("RoutePlanService using engine={}", this.engine);
    }

    @Transactional
    public RouteTask plan(Long orderId, RouteStrategy strategy,
                          double fromLon, double fromLat,
                          double toLon, double toLat,
                          List<double[]> via, List<String> avoid) {
        if (Double.isNaN(fromLon) || Double.isNaN(fromLat)
                || Double.isNaN(toLon) || Double.isNaN(toLat)) {
            throw new BizException(ErrorCode.POINT_INVALID, "invalid coordinate");
        }

        List<double[]> waypoints = new java.util.ArrayList<>();
        waypoints.add(new double[]{fromLon, fromLat});
        if (via != null) {
            waypoints.addAll(via);
        }
        waypoints.add(new double[]{toLon, toLat});

        OsrmClient.Record record = "osrm".equals(engine)
                ? osrmClient.planRoute(strategy, waypoints, avoid)
                : polylineEngine.planLine(strategy, waypoints, avoid);

        Geometry line = GeometryUtil.parseGeoJsonLine(record.geometry());

        RouteTask task = new RouteTask(orderId, strategy, line, record.distanceM(), record.durationS(), record.cost());
        task.setFromPoint(point(fromLon, fromLat));
        task.setToPoint(point(toLon, toLat));
        task.setViaPoints(waypoints.size() > 2 ? viaGeometry(waypoints) : emptyCollection());
        task.setAvoidSegments(avoid == null ? null : String.join(",", avoid));
        return routeTaskRepository.save(task);
    }

    private Geometry viaGeometry(List<double[]> waypoints) {
        int n = waypoints.size();
        if (n <= 2) return emptyCollection();
        org.locationtech.jts.geom.Coordinate[] coords = new org.locationtech.jts.geom.Coordinate[n - 2];
        for (int i = 1; i < n - 1; i++) {
            double[] w = waypoints.get(i);
            coords[i - 1] = new org.locationtech.jts.geom.Coordinate(w[0], w[1]);
        }
        return geometryFactory.createMultiPointFromCoords(coords);
    }

    private Geometry emptyCollection() {
        // 无途经点时存一个最小点占位（DB via_points 列 NOT NULL，且空集合序列化会溢出）
        return geometryFactory.createPoint(new Coordinate(0.0, 0.0));
    }
    private Point point(double lon, double lat) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }
}