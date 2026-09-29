package com.gis.logistics.gis.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;

/**
 * 空间几何工具：OSRM GeoJSON -> JTS Geometry（经 WKT 中转），零新增依赖。
 */
public final class GeometryUtil {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel());

    private GeometryUtil() {
    }

    public static Geometry parseGeoJsonLine(String geoJson) {
        try {
            JsonNode root = new ObjectMapper().readTree(geoJson);
            String type = root.path("type").asText();
            JsonNode coords = root.path("coordinates");
            String wkt = type.equalsIgnoreCase("LineString")
                    ? toLineString(coords)
                    : toMultiLineString(coords);
            return new WKTReader(FACTORY).read(wkt);
        } catch (Exception e) {
            throw new BizException(ErrorCode.ROUTE_PLAN_FAILED, "failed to parse geometry json", e);
        }
    }

    private static String toLineString(JsonNode coords) {
        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coords.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(coords.get(i).get(0).asDouble()).append(" ")
              .append(coords.get(i).get(1).asDouble());
        }
        return sb.append(")").toString();
    }

    private static String toMultiLineString(JsonNode coords) {
        StringBuilder sb = new StringBuilder("MULTILINESTRING((");
        for (int i = 0; i < coords.size(); i++) {
            if (i > 0) {
                sb.append("), (");
            }
            JsonNode line = coords.get(i);
            for (int j = 0; j < line.size(); j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append(line.get(j).get(0).asDouble()).append(" ")
                  .append(line.get(j).get(1).asDouble());
            }
        }
        return sb.append("))").toString();
    }
}
