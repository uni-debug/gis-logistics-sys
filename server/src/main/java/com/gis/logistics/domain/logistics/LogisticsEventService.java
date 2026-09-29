package com.gis.logistics.domain.logistics;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderService;
import com.gis.logistics.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 物流动态发布：写 LogisticsEvent + 触发订单状态迁移（物流一致性关键路径，需人工复核）。
 */
@Service
@RequiredArgsConstructor
public class LogisticsEventService {

    private final LogisticsEventRepository eventRepository;
    private final OrderService orderService;
    private final TrackStreamService trackStreamService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel());

    public record EventCommand(Long orderId, LogisticsEvent.EventType type,
                               Double lon, Double lat, Long operatorId) {}

    @Transactional
    public LogisticsEvent publish(EventCommand cmd) {
        OrderStatus target = mapToOrderStatus(cmd.type());
        if (target != null) {
            orderService.transition(cmd.orderId(), target, cmd.operatorId());
        }
        var event = eventRepository.save(new LogisticsEvent(cmd.orderId(), cmd.type(),
                point(cmd.lon(), cmd.lat()), cmd.operatorId()));
        trackStreamService.broadcast(cmd.orderId(), event);
        return event;
    }

    private org.locationtech.jts.geom.Point point(Double lon, Double lat) {
        if (lon == null || lat == null) {
            return null;
        }
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }

    private OrderStatus mapToOrderStatus(LogisticsEvent.EventType type) {
        return switch (type) {
            case PICKED -> OrderStatus.PICKED;
            case IN_TRANSIT -> OrderStatus.IN_TRANSIT;
            case ARRIVED_DELIVERY -> OrderStatus.ARRIVED;
            case DELIVERED -> OrderStatus.DELIVERED;
        };
    }

    public List<LogisticsEvent> trackOf(Long orderId) {
        return eventRepository.findByOrderIdOrderByOccurredAtAsc(orderId);
    }
}
