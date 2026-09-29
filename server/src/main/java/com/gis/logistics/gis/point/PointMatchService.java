package com.gis.logistics.gis.point;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 点位最近邻匹配（GIS 关键路径，需人工复核）。
 * STRtree 空间索引 + 距离阈值，用于站点/中转点匹配。
 */
@Service
@RequiredArgsConstructor
public class PointMatchService {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel());

    public record Candidate(String name, double lon, double lat) {}
    public record MatchResult(Candidate candidate, double distanceM) {}

    /**
     * 在候选点集中找离 (lon,lat) 最近且距离 <= radiusM 的点。
     * 候选集为空或超出半径返回 null。
     */
    public MatchResult nearest(List<Candidate> candidates, double lon, double lat, double radiusM) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        STRtree tree = new STRtree();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            Point p = geometryFactory.createPoint(new Coordinate(c.lon(), c.lat()));
            tree.insert(p.getEnvelopeInternal(), i);
        }
        tree.build();

        double degRadius = radiusM / 111320.0;
        Envelope searchEnv = new Envelope(
                lon - degRadius, lon + degRadius,
                lat - degRadius, lat + degRadius);

        List<Integer> hits = tree.query(searchEnv);
        Candidate best = null;
        double bestDist = Double.MAX_VALUE;
        for (int idx : hits) {
            Candidate c = candidates.get(idx);
            double dist = haversineM(lat, lon, c.lat(), c.lon());
            if (dist <= radiusM && dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best == null ? null : new MatchResult(best, bestDist);
    }

    private static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dphi = Math.toRadians(lat2 - lat1);
        double dlam = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dphi / 2) * Math.sin(dphi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlam / 2) * Math.sin(dlam / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
