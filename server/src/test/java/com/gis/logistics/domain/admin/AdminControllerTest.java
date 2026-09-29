package com.gis.logistics.domain.admin;

import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderService;
import com.gis.logistics.domain.order.OrderStatus;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private UserRepository userRepository;
    @Mock private OrderStatsService orderStatsService;
    @Mock private com.gis.logistics.domain.staff.StaffService staffService;
    @Mock private com.gis.logistics.domain.review.ReviewService reviewService;
    @Mock private com.gis.logistics.domain.review.ReviewRepository reviewRepository;
    @Mock private com.gis.logistics.domain.feedback.FeedbackService feedbackService;
    @Mock private com.gis.logistics.domain.im.ImService imService;
    @Mock private com.gis.logistics.domain.warehouse.WarehouseService warehouseService;
    @Mock private com.gis.logistics.domain.demand.DemandService demandService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private com.gis.logistics.common.crypto.PhoneCipher phoneCipher;
    @Mock private RouteStatsService routeStatsService;
    @Mock private com.gis.logistics.domain.staffapp.StaffApplicationService applicationService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(orderRepository, orderService, userRepository, orderStatsService, staffService, reviewService, reviewRepository, feedbackService, imService, warehouseService, demandService, passwordEncoder, phoneCipher, routeStatsService, applicationService);
    }

    @Test
    void ordersReturnsPage() {
        when(orderRepository.findByFilter(isNull(), isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order()), PageRequest.of(0, 20), 1));
        var resp = controller.orders(null, null, null, null, null, 0, 20);
        assertEquals(1, resp.getData().getContent().size());
    }

    @Test
    void userStatusToggles() {
        User u = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var resp = controller.setUserStatus(1L, new AdminController.UserStatusBody(0));
        assertEquals(0, resp.getData().getStatus());
    }

    @Test
    void adminTransitionDelegatesToOrderService() {
        Order after = order();
        after.setStatus(OrderStatus.PAID);
        when(orderService.transition(eq(1L), eq(OrderStatus.PAID), eq(0L))).thenReturn(after);
        var resp = controller.adminTransition(1L, new AdminController.OrderStatusBody(OrderStatus.PAID));
        assertEquals(OrderStatus.PAID, resp.getData().getStatus());
    }

    @Test
    void statsClampedToRange() {
        when(orderStatsService.snapshot(anyInt())).thenAnswer(inv -> new OrderStatsService.StatsSnapshot(List.of(), List.of(), java.util.Map.of()));
        controller.orderStats(0);
        verify(orderStatsService).snapshot(1);
    }

    private Order order() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(OrderStatus.PENDING);
        o.setUserId(10L);
        o.setStaffId(20L);
        o.setDemandId(30L);
        o.setAmount(500);
        o.setCreatedAt(LocalDateTime.now());
        return o;
    }

    private User user() {
        User u = new User();
        u.setId(1L);
        u.setStatus(1);
        return u;
    }
}














