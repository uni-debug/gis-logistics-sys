package com.gis.logistics.domain.admin;

/**
 * 月度订单聚合结果（SQL 聚合，供 ECharts）。ym 形如 2026-09。
 */
public interface MonthAggregation {
    String getYm();
    Long getOrderCount();
    Long getTotalFen();
}
