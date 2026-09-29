package com.gis.logistics.domain.staff;

import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandRepository;
import com.gis.logistics.domain.demand.DemandService;
import com.gis.logistics.domain.logistics.LogisticsEventService;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderService;
import com.gis.logistics.domain.order.OrderStatus;
import com.gis.logistics.gis.route.RoutePlanService;
import com.gis.logistics.gis.route.RouteTask;
import com.gis.logistics.gis.route.RouteTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffControllerTest {

    @Mock private StaffProfileRepository staffProfileRepository;
    @Mock private DemandService demandService;
    @Mock private DemandRepository demandRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private RoutePlanService routePlanService;
    @Mock private RouteTaskRepository routeTaskRepository;
    @Mock private LogisticsEventService logisticsEventService;
    @Mock private com.gis.logistics.domain.logistics.LogisticsEventRepository logisticsEventRepository;
    @Mock private com.gis.logistics.domain.logistics.TrackStreamService trackStreamService;
    @Mock private com.gis.logistics.domain.review.ReviewRepository reviewRepository;
    @Mock private com.gis.logistics.domain.review.ReviewService reviewService;
    @Mock private com.gis.logistics.domain.warehouse.SiteRepository siteRepository;
    @Mock private com.gis.logistics.domain.user.UserRepository userRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private StaffController controller;

    @BeforeEach
    void setUp() {
        lenient().when(staffProfileRepository.findByUserId(anyLong()))
                .thenAnswer(inv -> {
                    StaffProfile sp = new StaffProfile();
                    sp.setId(99L);
                    sp.setUserId(inv.getArgument(0, Long.class));
                    return java.util.Optional.of(sp);
                });
        controller = new StaffController(staffProfileRepository, demandService, demandRepository, orderRepository,
                orderService, routePlanService, routeTaskRepository, logisticsEventService,
                logisticsEventRepository, trackStreamService, reviewRepository, reviewService,
                siteRepository, userRepository, passwordEncoder);
    }

    @Test
    void quoteDelegatesToService() {
        when(demandService.quote(anyLong(), anyLong(), anyInt())).thenAnswer(inv -> {
            Demand d = new Demand();
            d.setId(1L);
            d.setQuotedPrice(inv.getArgument(2, Integer.class));
            return d;
        });

        var resp = controller.quote(1L, new StaffController.QuoteBody(5000), fakeAuth(2L));
        assertEquals(5000, resp.getData().getQuotedPrice());
        verify(demandService).quote(1L, 99L, 5000);
    }

    @Test
    void planRouteDelegates() {
        when(orderRepository.findById(1L)).thenAnswer(inv -> {
            Order o = new Order();
            o.setId(1L);
            o.setStaffId(99L);
            o.setFromLon(java.math.BigDecimal.valueOf(116.4));
            o.setFromLat(java.math.BigDecimal.valueOf(39.9));
            o.setToLon(java.math.BigDecimal.valueOf(116.5));
            o.setToLat(java.math.BigDecimal.valueOf(39.95));
            return java.util.Optional.of(o);
        });
        when(routePlanService.plan(anyLong(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()))
                .thenAnswer(inv -> {
                    RouteTask t = new RouteTask(1L, null, null, 0, 0, 0);
                    t.setId(5L);
                    return t;
                });
        var body = new StaffController.PlanRouteBody(
                com.gis.logistics.gis.route.RouteStrategy.FASTEST,
                116.4, 39.9, 116.5, 39.95, List.of(), List.of());
        var resp = controller.planRoute(1L, body, fakeAuth(99L));
        assertEquals(5L, resp.getData().getId());
    }

    @Test
    void latestRouteEmptyThrowsNotFound() {
        when(routeTaskRepository.findByOrderIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertThrows(com.gis.logistics.common.exception.BizException.class,
                () -> controller.latestRoute(1L));
    }

    @Test
    void publishEventDelegates() {
        var sample = new com.gis.logistics.domain.logistics.LogisticsEvent();
        when(logisticsEventService.publish(any())).thenReturn(sample);
        var body = new StaffController.EventBody(com.gis.logistics.domain.logistics.LogisticsEvent.EventType.PICKED, 116.4, 39.9);
        var resp = controller.publishEvent(1L, body, fakeAuth(2L));
        assertSame(sample, resp.getData());
        verify(logisticsEventService).publish(any());
    }

    @Test
    void orderEventsReturnsList() {
        Order o = new Order();
        o.setId(1L);
        o.setStaffId(99L);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(o));
        when(logisticsEventRepository.findByOrderIdOrderByOccurredAtAsc(1L)).thenReturn(List.of());
        var resp = controller.orderEvents(1L, fakeAuth(2L));
        assertEquals(0, resp.getData().size());
    }

    @Test
    void orderEventsRejectsOtherStaff() {
        Order o = new Order();
        o.setId(1L);
        o.setStaffId(77L);
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(o));
        assertThrows(com.gis.logistics.common.exception.BizException.class,
                () -> controller.orderEvents(1L, fakeAuth(2L)));
    }

    private org.springframework.security.core.Authentication fakeAuth(Long staffId) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                staffId, null, List.of());
    }
}






