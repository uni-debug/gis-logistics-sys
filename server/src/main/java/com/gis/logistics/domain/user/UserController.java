package com.gis.logistics.domain.user;

import com.gis.logistics.common.web.ApiResponse;
import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandService;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.gis.route.RouteTask;
import com.gis.logistics.gis.route.RouteTaskRepository;
import com.gis.logistics.domain.payment.Payment;
import com.gis.logistics.domain.payment.PaymentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final DemandService demandService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final com.gis.logistics.domain.logistics.LogisticsEventService logisticsEventService;
    private final com.gis.logistics.domain.logistics.LogisticsEventRepository logisticsEventRepository;
    private final RouteTaskRepository routeTaskRepository;
    private final com.gis.logistics.domain.logistics.TrackStreamService trackStreamService;
    private final PaymentService paymentService;
    private final com.gis.logistics.domain.review.ReviewService reviewService;
    private final com.gis.logistics.domain.feedback.FeedbackService feedbackService;
    private final com.gis.logistics.domain.im.ImService imService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.gis.logistics.domain.review.ReviewRepository reviewRepository;
    private final com.gis.logistics.domain.feedback.FeedbackRepository feedbackRepository;
    private final com.gis.logistics.common.crypto.PhoneCipher phoneCipher;
    private final com.gis.logistics.domain.staffapp.StaffApplicationService applicationService;
    private final com.gis.logistics.domain.warehouse.SiteRepository siteRepository;
    private final com.gis.logistics.domain.staffapp.StaffApplicationRepository applicationRepository;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId).orElseThrow();
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("role", u.getRole().name());
        String phonePlain;
        try {
            String phoneB64 = u.getPhoneEnc() == null ? "" : java.util.Base64.getEncoder().encodeToString(u.getPhoneEnc());
            phonePlain = phoneCipher.decrypt(phoneB64);
        } catch (Exception e) {
            // 密钥不匹配或数据损坏时降级，不阻断资料查看
            phonePlain = "";
        }
        m.put("phoneMasked", com.gis.logistics.common.crypto.PhoneMasker.mask(phonePlain));
        m.put("createdAt", u.getCreatedAt());
        return ApiResponse.ok(m);
    }

    @PatchMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody @jakarta.validation.Valid UpdateProfileBody body,
                                                          Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_BAD_REQUEST, "user not found"));
        if (body.name() != null && !body.name().isBlank()) {
            u.setName(body.name().trim());
        }
        User saved = userRepository.save(u);
        return ApiResponse.ok(Map.of("id", saved.getId(), "name", saved.getName()));
    }

    @PostMapping("/password")
    public ApiResponse<Map<String, Object>> changePassword(@RequestBody @jakarta.validation.Valid ChangePasswordBody body,
                                                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId).orElseThrow();
        String oldHash = u.getPasswordHash();
        if (oldHash == null || !passwordEncoder.matches(body.oldPassword(), oldHash)) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.AUTH_TOKEN_INVALID, "old password incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        userRepository.save(u);
        return ApiResponse.ok(Map.of("changed", true));
    }

    @PostMapping("/demands")
    public ApiResponse<Demand> publish(@RequestBody @jakarta.validation.Valid PublishDemandBody body,
                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Demand d = demandService.publish(userId, new DemandService.PublishCommand(
                body.title(), body.weightG(), body.volumeCm3(), body.fragile(),
                body.originRegion(), body.originAddr(), body.targetRegion(), body.targetAddr()));
        return ApiResponse.ok(d);
    }

    @GetMapping("/demands")
    public ApiResponse<Page<Demand>> myDemands(@RequestParam(required = false) Demand.Status status,
                                               Authentication auth,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getPrincipal();
        Page<Demand> result = demandService.pageMine(userId, status, PageRequest.of(page, size));
        return ApiResponse.ok(result);
    }

    @GetMapping("/orders")
    public ApiResponse<Page<Order>> myOrders(@RequestParam(required = false) com.gis.logistics.domain.order.OrderStatus status,
                                             Authentication auth,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getPrincipal();
        Page<Order> result = (status == null)
                ? orderRepository.findByUserId(userId, PageRequest.of(page, size))
                : orderRepository.findByUserIdAndStatus(userId, status, PageRequest.of(page, size));
        return ApiResponse.ok(result);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<Order> myOrderDetail(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Order o = orderRepository.findById(id)
                .filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found"));
        return ApiResponse.ok(o);
    }

    @GetMapping("/orders/{id}/track-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter trackStream(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        if (!userId.equals(o.getUserId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        return trackStreamService.subscribe(id);
    }

    @GetMapping("/orders/{id}/route")
    public ApiResponse<com.gis.logistics.gis.route.RouteTask> latestRoute(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        if (!userId.equals(o.getUserId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        var tasks = routeTaskRepository.findByOrderIdOrderByCreatedAtDesc(id);
        if (tasks.isEmpty()) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ROUTE_PLAN_FAILED, "no route yet");
        }
        return ApiResponse.ok(tasks.get(0));
    }

    @PostMapping("/orders/{id}/pay")
    public ApiResponse<Payment> pay(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(paymentService.initiate(id, userId, Payment.Channel.ALIPAY));
    }

    @PostMapping("/orders/{id}/pay/callback")
    public ApiResponse<Payment> payCallback(@PathVariable Long id, @RequestBody @jakarta.validation.Valid PayCallbackBody body,
                                            Authentication auth) {
        return ApiResponse.ok(paymentService.handleCallback(body.txnNo(), id, body.amountFen(), true));
    }

    @PostMapping("/demands/{id}/confirm")
    public ApiResponse<com.gis.logistics.domain.order.Order> confirmDemand(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        var cr = demandService.confirm(id, userId);
        return ApiResponse.ok(cr.order());
    }

    @GetMapping("/orders/{id}/logistics")
    public ApiResponse<java.util.List<com.gis.logistics.domain.logistics.LogisticsEvent>> orderLogistics(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        orderRepository.findById(id).filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found"));
        return ApiResponse.ok(logisticsEventService.trackOf(id));
    }

    @PostMapping("/orders/{id}/review")
    public ApiResponse<com.gis.logistics.domain.review.Review> submitReview(@PathVariable Long id,
                                                                            @RequestBody @jakarta.validation.Valid ReviewBody body,
                                                                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Order o = orderRepository.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found"));
        com.gis.logistics.domain.review.Review r = reviewService.create(id, userId, o.getStaffId(), body.rating(), body.content());
        return ApiResponse.ok(r);
    }

    @GetMapping("/reviews")
    public ApiResponse<java.util.List<com.gis.logistics.domain.review.Review>> myReviews(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(reviewRepository.findByUserIdAndDeletedFalse(userId));
    }

    @GetMapping("/feedbacks")
    public ApiResponse<java.util.List<com.gis.logistics.domain.feedback.Feedback>> myFeedbacks(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(feedbackRepository.findByUserId(userId));
    }

    @PostMapping("/feedback")
    public ApiResponse<com.gis.logistics.domain.feedback.Feedback> submitFeedback(@RequestBody @jakarta.validation.Valid FeedbackBody body,
                                                                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(feedbackService.submit(userId, body.type(), body.content()));
    }

    @PostMapping("/im/sessions")
    public ApiResponse<com.gis.logistics.domain.im.ImSession> openIm(@RequestBody @jakarta.validation.Valid ImOpenBody body,
                                                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(imService.openSession(userId));
    }

    @PostMapping("/im/sessions/{id}/messages")
    public ApiResponse<com.gis.logistics.domain.im.ImMessage> sendIm(@PathVariable Long id,
                                                                     @RequestBody @jakarta.validation.Valid ImMsgBody body,
                                                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        com.gis.logistics.domain.im.ImSession s = imService.activeSessions().stream()
                .filter(x -> x.getUserId().equals(userId))
                .filter(x -> x.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_NOT_FOUND, "session not found"));
        return ApiResponse.ok(imService.send(id, com.gis.logistics.domain.im.ImMessage.SenderRole.USER, body.content()));
    }

    @GetMapping("/im/sessions/{id}/messages")
    public ApiResponse<java.util.List<com.gis.logistics.domain.im.ImMessage>> imHistory(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(imService.history(id));
    }

    @GetMapping("/sites")
    public ApiResponse<java.util.List<com.gis.logistics.domain.warehouse.Site>> sites() {
        return ApiResponse.ok(siteRepository.findByStatus(1));
    }

    @PostMapping("/staff-application")
    public ApiResponse<com.gis.logistics.domain.staffapp.StaffApplication> submitStaffApplication(
            @RequestBody @jakarta.validation.Valid StaffApplicationBody body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(applicationService.submit(userId, body.siteId(), body.licenseNo(), body.reason()));
    }

    @GetMapping("/staff-application")
    public ApiResponse<com.gis.logistics.domain.staffapp.StaffApplication> myStaffApplication(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(applicationRepository.findByUserId(userId).orElse(null));
    }

    @DeleteMapping("/staff-application")
    public ApiResponse<Void> cancelStaffApplication(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        applicationService.cancel(userId);
        return ApiResponse.ok(null);
    }

    public record UpdateProfileBody(String name) {}
    public record ChangePasswordBody(
            @NotBlank String oldPassword,
            @NotBlank @jakarta.validation.constraints.Size(min = 6, max = 64) String newPassword) {}

    public record PublishDemandBody(
            @NotBlank String title,
            @Positive int weightG,
            @Positive int volumeCm3,
            boolean fragile,
            @NotBlank String originRegion,
            @NotBlank String originAddr,
            @NotBlank String targetRegion,
            @NotBlank String targetAddr) {}

    public record ReviewBody(@jakarta.validation.constraints.Positive int rating, String content) {}
    public record FeedbackBody(com.gis.logistics.domain.feedback.Feedback.Type type,
                               @jakarta.validation.constraints.NotBlank String content) {}
    public record ImOpenBody() {}
    public record ImMsgBody(@jakarta.validation.constraints.NotBlank String content) {}
    public record StaffApplicationBody(@Positive Long siteId, @NotBlank String licenseNo, String reason) {}
    public record PayCallbackBody(@jakarta.validation.constraints.NotBlank String txnNo,
                                  @jakarta.validation.constraints.Positive int amountFen) {}
}

