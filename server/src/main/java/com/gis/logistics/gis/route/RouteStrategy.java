package com.gis.logistics.gis.route;

/**
 * 路线规划策略：SHORTEST 最短距离 / FASTEST 最快时效 / CHEAPEST 最低成本。
 * CHEAPEST 在 OSRM 距离结果上叠加成本模型（燃油+规避惩罚）。
 */
public enum RouteStrategy {
    SHORTEST, FASTEST, CHEAPEST
}
