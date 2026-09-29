package com.gis.logistics.domain.admin;

import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandRepository;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单统计：月订单量/金额走 SQL 聚合，区域分布与热门路线在内存聚合（order -> demand 的 region）。
 */
@Service
@RequiredArgsConstructor
public class OrderStatsService {

    private final OrderRepository orderRepository;
    private final DemandRepository demandRepository;

    public record MonthStat(YearMonth month, long orderCount, long totalAmountFen) {}
    public record RegionStat(String region, long orderCount) {}
    public record StatsSnapshot(List<MonthStat> monthly, List<RegionStat> byRegion, Map<String, Long> topRoutes) {}

    public StatsSnapshot snapshot(int monthsBack) {
        List<MonthAggregation> rows = orderRepository.aggregateMonthly();
        YearMonth cutoff = YearMonth.now().minusMonths(monthsBack - 1L);
        List<MonthStat> monthly = rows.stream()
                .filter(r -> YearMonth.parse(r.getYm()).compareTo(cutoff) >= 0)
                .map(r -> new MonthStat(YearMonth.parse(r.getYm()),
                        r.getOrderCount(), r.getTotalFen()))
                .toList();

        // in-memory region + route aggregation
        List<Order> all = orderRepository.findAll();
        Map<Long, Demand> demandCache = new HashMap<>();
        demandRepository.findAll().forEach(d -> demandCache.put(d.getId(), d));

        Map<String, Long> regionCount = new HashMap<>();
        Map<String, Long> routeCount = new HashMap<>();
        for (Order o : all) {
            Demand d = demandCache.get(o.getDemandId());
            if (d == null) continue;
            regionCount.merge(d.getOriginRegion(), 1L, Long::sum);
            regionCount.merge(d.getTargetRegion(), 1L, Long::sum);
            String route = d.getOriginRegion() + " -> " + d.getTargetRegion();
            routeCount.merge(route, 1L, Long::sum);
        }

        List<RegionStat> byRegion = regionCount.entrySet().stream()
                .map(e -> new RegionStat(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(RegionStat::orderCount).reversed())
                .toList();

        Map<String, Long> topRoutes = routeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

        return new StatsSnapshot(monthly, byRegion, topRoutes);
    }
}
