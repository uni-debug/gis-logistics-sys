package com.gis.logistics.gis.track;

import com.fasterxml.jackson.databind.JsonNode;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.logistics.LogisticsEvent;
import com.gis.logistics.domain.logistics.LogisticsEventRepository;
import com.gis.logistics.gis.osrm.OsrmClient;
import com.gis.logistics.gis.util.GeometryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 轨迹吸附服务（GIS 关键路径，需人工复核）。
 * 原始 GPS 点 -> OSRM match -> 吸附 LineString + 逐点 LogisticsEvent，供用户端轨迹追踪。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackMatchService {

    private final OsrmClient osrmClient;
    private final LogisticsEventRepository eventRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel());

    public record TrackPoint(double lon, double lat) {}
    public record MatchResult(Geometry snappedLine, int pointCount) {}

    @Transactional
    public MatchResult track(Long orderId, List<TrackPoint> points, Long operatorId) {
        if (points == null || points.size() < 2) {
            throw new BizException(ErrorCode.TRACK_MATCH_FAILED, "need at least 2 gps points");
        }
        double[] flat = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            flat[i * 2] = points.get(i).lon();
            flat[i * 2 + 1] = points.get(i).lat();
        }
        JsonNode match = osrmClient.matchTrack(flat);
        JsonNode matching = match.path("matchings").get(0);
        if (matching == null) {
            throw new BizException(ErrorCode.TRACK_MATCH_FAILED, "osrm match returned no matchings");
        }
        Geometry line = GeometryUtil.parseGeoJsonLine(matching.get("geometry").toString());

        for (TrackPoint p : points) {
            LogisticsEvent event = new LogisticsEvent(orderId,
                    LogisticsEvent.EventType.IN_TRANSIT,
                    geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(p.lon(), p.lat())),
                    operatorId);
            eventRepository.save(event);
        }
        return new MatchResult(line, points.size());
    }
}
