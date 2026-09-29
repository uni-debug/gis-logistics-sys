package com.gis.logistics.domain.admin;

import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandRepository;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.gis.route.RouteTask;
import com.gis.logistics.gis.route.RouteTaskRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路线管理（GIS 关键路径，需人工复核）：聚合 route_tasks 的统计与可视化数据。
 * 返回结构：summary / topByDistance / recent / strategyDistribution。
 */
@Service
@RequiredArgsConstructor
public class RouteStatsService {

    private final RouteTaskRepository routeTaskRepository;
    private final OrderRepository orderRepository;
    private final DemandRepository demandRepository;

    public record Summary(long total, double avgDistanceKm, double avgEtaSeconds,
                          Map<String, Long> strategyDistribution) {}

    public record RouteDto(long taskId, long orderId, String strategy,
                           String fromRegion, String toRegion,
                           double fromLon, double fromLat, double toLon, double toLat,
                           double distanceKm, int etaSeconds, long costFen,
                           String createdAt, List<double[]> geometry, List<double[]> via) {}

    public record RouteSnapshot(Summary summary, List<RouteDto> topByDistance,
                                List<RouteDto> recent) {}

    public RouteSnapshot snapshot(int limit) {
        List<RouteTask> all = routeTaskRepository.findAll();
        long total = all.size();
        double avgKm = 0, avgEta = 0;
        Map<String, Long> strategy = new HashMap<>();
        if (total > 0) {
            double sumKm = 0; double sumEta = 0;
            for (RouteTask t : all) {
                if (t.getDistanceM() != null) sumKm += t.getDistanceM() / 1000.0;
                if (t.getEtaS() != null) sumEta += t.getEtaS();
                strategy.merge(t.getStrategy().name(), 1L, Long::sum);
            }
            avgKm = sumKm / total;
            avgEta = sumEta / total;
        }

        List<RouteTask> topTasks = all.stream()
                .filter(t -> t.getDistanceM() != null)
                .sorted((a, b) -> Double.compare(b.getDistanceM(), a.getDistanceM()))
                .limit(10)
                .toList();
        List<RouteTask> recent = routeTaskRepository
                .findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, limit));

        return new RouteSnapshot(
                new Summary(total, avgKm, avgEta, strategy),
                topTasks.stream().map(this::toDto).toList(),
                recent.stream().map(this::toDto).toList());
    }

    private RouteDto toDto(RouteTask t) {
        List<double[]> geo = toLngLat(t.getGeometry());
        List<double[]> via = toLngLat(t.getViaPoints());
        // viaPoints may be a single placeholder (0,0) point when there is no via; filter it out
        if (via.size() == 1 && via.get(0)[0] == 0.0 && via.get(0)[1] == 0.0) via = new ArrayList<>();
        String[] regions = regionsOf(t.getOrderId());
        return new RouteDto(
                t.getId(), t.getOrderId(), t.getStrategy().name(),
                regions[0], regions[1],
                pointLon(t.getFromPoint()), pointLat(t.getFromPoint()),
                pointLon(t.getToPoint()), pointLat(t.getToPoint()),
                t.getDistanceM() == null ? 0 : t.getDistanceM() / 1000.0,
                t.getEtaS() == null ? 0 : t.getEtaS(),
                t.getCost() == null ? 0L : t.getCost().longValue(),
                t.getCreatedAt() == null ? "" : t.getCreatedAt().toString(),
                geo, via);
    }

    private List<double[]> toLngLat(Geometry g) {
        if (g == null) return new ArrayList<>();
        List<double[]> out = new ArrayList<>();
        if (g.getGeometryType().equals("MultiLineString")
                || g.getGeometryType().equals("GeometryCollection")) {
            for (int i = 0; i < g.getNumGeometries(); i++) {
                Coordinate[] sub = g.getGeometryN(i).getCoordinates();
                for (Coordinate c : sub) out.add(new double[]{c.x, c.y});
            }
        } else {
            Coordinate[] coords = g.getCoordinates();
            for (Coordinate c : coords) out.add(new double[]{c.x, c.y});
        }
        return out;
    }

    private double pointLon(Geometry g) { return g == null ? 0 : g.getCoordinate().x; }
    private double pointLat(Geometry g) { return g == null ? 0 : g.getCoordinate().y; }

    /** orderId -> (originRegion, targetRegion)，查不到时降级为空串 */
    private String[] regionsOf(Long orderId) {
        try {
            Order o = orderRepository.findById(orderId).orElse(null);
            if (o == null) return new String[] {"", ""};
            Demand d = demandRepository.findById(o.getDemandId()).orElse(null);
            if (d == null) return new String[] {"", ""};
            return new String[] { d.getOriginRegion() == null ? "" : d.getOriginRegion(),
                                  d.getTargetRegion() == null ? "" : d.getTargetRegion() };
        } catch (Exception e) {
            return new String[] {"", ""};
        }
    }
}
