package com.gis.logistics.domain.user;

import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandService;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private DemandService demandService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock private com.gis.logistics.domain.logistics.LogisticsEventService logisticsEventService;
    @Mock private com.gis.logistics.domain.logistics.LogisticsEventRepository logisticsEventRepository;
    @Mock private com.gis.logistics.domain.review.ReviewService reviewService;
    @Mock private com.gis.logistics.domain.feedback.FeedbackService feedbackService;
    @Mock private com.gis.logistics.domain.im.ImService imService;
    @Mock private com.gis.logistics.gis.route.RouteTaskRepository routeTaskRepository;
@Mock private com.gis.logistics.domain.logistics.TrackStreamService trackStreamService;
    @Mock private com.gis.logistics.domain.payment.PaymentService paymentService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private com.gis.logistics.domain.review.ReviewRepository reviewRepository;
    @Mock private com.gis.logistics.domain.feedback.FeedbackRepository feedbackRepository;
    @Mock private com.gis.logistics.common.crypto.PhoneCipher phoneCipher;
    @Mock private com.gis.logistics.domain.staffapp.StaffApplicationService applicationService;
    @Mock private com.gis.logistics.domain.warehouse.SiteRepository siteRepository;
    @Mock private com.gis.logistics.domain.staffapp.StaffApplicationRepository applicationRepository;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(demandService, orderRepository, userRepository,
                logisticsEventService, logisticsEventRepository, routeTaskRepository,
                trackStreamService, paymentService, reviewService, feedbackService, imService,
                passwordEncoder, reviewRepository, feedbackRepository, phoneCipher,
                applicationService, siteRepository, applicationRepository);
    }

    @Test
    void publishDelegatesToService() {
        when(demandService.publish(anyLong(), any(DemandService.PublishCommand.class)))
                .thenAnswer(inv -> {
                    Demand d = new Demand();
                    d.setId(1L);
                    d.setStatus(Demand.Status.PENDING);
                    return d;
                });

        UserController.PublishDemandBody body = new UserController.PublishDemandBody(
                "寄件", 500, 1000, false, "110000", "北京", "310000", "上海");

        var resp = controller.publish(body, fakeAuth(10L));
        assertEquals(1L, resp.getData().getId());
        verify(demandService).publish(eq(10L), any(DemandService.PublishCommand.class));
    }

    @Test
    void myOrdersReturnsPage() {
        when(orderRepository.findByUserIdAndStatus(eq(10L), any(), any()))
                .thenReturn(new PageImpl<>(List.of(order()), PageRequest.of(0, 20), 1));

        var resp = controller.myOrders(OrderStatus.PENDING, fakeAuth(10L), 0, 20);
        assertEquals(1, resp.getData().getContent().size());
    }

    @Test
    void orderDetailRejectsForeignOrder() {
        Order o = order();
        o.setUserId(99L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
        assertThrows(com.gis.logistics.common.exception.BizException.class,
                () -> controller.myOrderDetail(1L, fakeAuth(10L)));
    }

    @Test
    void orderDetailReturnsOwnOrder() {
        Order o = order();
        o.setUserId(10L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

        var resp = controller.myOrderDetail(1L, fakeAuth(10L));
        assertEquals(1L, resp.getData().getId());
    }

    @Test
    void submitReviewDelegates() {
        Order o = order();
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(o));
        when(reviewService.create(anyLong(), anyLong(), anyLong(), anyInt(), any())).thenAnswer(inv -> {
            com.gis.logistics.domain.review.Review r = new com.gis.logistics.domain.review.Review(1L, 10L, 20L, 5, "x");
            r.setId(1L);
            return r;
        });
        var body = new UserController.ReviewBody(5, "good");
        var resp = controller.submitReview(1L, body, fakeAuth(10L));
        assertEquals(5, resp.getData().getRating());
    }

    @Test
    void submitFeedbackDelegates() {
        when(feedbackService.submit(anyLong(), any(), anyString())).thenAnswer(inv -> {
            com.gis.logistics.domain.feedback.Feedback f = new com.gis.logistics.domain.feedback.Feedback(10L, com.gis.logistics.domain.feedback.Feedback.Type.COMPLAIN, "c");
            f.setId(1L);
            return f;
        });
        var body = new UserController.FeedbackBody(com.gis.logistics.domain.feedback.Feedback.Type.COMPLAIN, "投诉");
        var resp = controller.submitFeedback(body, fakeAuth(10L));
        assertEquals(1L, resp.getData().getId());
    }

    @Test
    void openImDelegates() {
        when(imService.openSession(10L)).thenAnswer(inv -> {
            com.gis.logistics.domain.im.ImSession s = new com.gis.logistics.domain.im.ImSession(10L);
            s.setId(1L);
            return s;
        });
        var resp = controller.openIm(new UserController.ImOpenBody(), fakeAuth(10L));
        assertEquals(1L, resp.getData().getId());
    }

    private Order order() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(OrderStatus.PENDING);
        o.setUserId(10L);
        o.setStaffId(20L);
        o.setDemandId(30L);
        o.setAmount(1000);
        return o;
    }

    @Test
    void confirmDemandCreatesOrder() {
        Long userId = 10L;
        var cr = new com.gis.logistics.domain.demand.DemandService.ConfirmResult(new com.gis.logistics.domain.demand.Demand(), new com.gis.logistics.domain.order.Order());
        when(demandService.confirm(1L, userId)).thenReturn(cr);
        var resp = controller.confirmDemand(1L, fakeAuth(userId));
        assertSame(cr.order(), resp.getData());
        verify(demandService).confirm(1L, userId);
    }

    private org.springframework.security.core.Authentication fakeAuth(Long userId) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userId, null, List.of());
    }
}


