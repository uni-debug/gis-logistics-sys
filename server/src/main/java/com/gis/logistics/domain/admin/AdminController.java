package com.gis.logistics.domain.admin;

import com.gis.logistics.common.web.ApiResponse;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.order.OrderService;
import com.gis.logistics.domain.order.OrderStatus;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderStatsService orderStatsService;
    private final com.gis.logistics.domain.staff.StaffService staffService;
    private final com.gis.logistics.domain.review.ReviewService reviewService;
    private final com.gis.logistics.domain.review.ReviewRepository reviewRepository;
    private final com.gis.logistics.domain.feedback.FeedbackService feedbackService;
    private final com.gis.logistics.domain.im.ImService imService;
    private final com.gis.logistics.domain.warehouse.WarehouseService warehouseService;
    private final com.gis.logistics.domain.demand.DemandService demandService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.gis.logistics.common.crypto.PhoneCipher phoneCipher;
    private final com.gis.logistics.domain.admin.RouteStatsService routeStatsService;
    private final com.gis.logistics.domain.staffapp.StaffApplicationService applicationService;

    @GetMapping("/staff-applications")
    public ApiResponse<java.util.List<com.gis.logistics.domain.staffapp.StaffApplication>> staffApplications(
            @RequestParam(required = false) com.gis.logistics.domain.staffapp.StaffApplication.Status status) {
        return ApiResponse.ok(applicationService.byStatus(status));
    }

    @PostMapping("/staff-applications/{id}/approve")
    public ApiResponse<com.gis.logistics.domain.staffapp.StaffApplication> approveApplication(
            @PathVariable Long id, Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(applicationService.approve(id, adminId));
    }

    @PostMapping("/staff-applications/{id}/reject")
    public ApiResponse<com.gis.logistics.domain.staffapp.StaffApplication> rejectApplication(
            @PathVariable Long id, @RequestBody(required = false) AppRejectBody body, Authentication auth) {
        Long adminId = (Long) auth.getPrincipal();
        return ApiResponse.ok(applicationService.reject(id, adminId, body == null ? null : body.reason()));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<Order>> orders(@RequestParam(required = false) OrderStatus status,
                                           @RequestParam(required = false) Long staffId,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> result = orderRepository.findByFilter(status, staffId, userId, from, to, pageable);
        return ApiResponse.ok(result);
    }

    @GetMapping("/orders/stats")
    public ApiResponse<OrderStatsService.StatsSnapshot> orderStats(@RequestParam(defaultValue = "12") int months) {
        return ApiResponse.ok(orderStatsService.snapshot(Math.max(1, Math.min(60, months))));
    }

    @GetMapping("/users")
    public ApiResponse<Page<User>> users(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<User> result = userRepository.findAll(PageRequest.of(page, size));
        return ApiResponse.ok(result);
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<User> setUserStatus(@PathVariable Long id, @RequestBody @jakarta.validation.Valid UserStatusBody body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_NOT_FOUND, "user not found"));
        user.setStatus(body.status());
        return ApiResponse.ok(userRepository.save(user));
    }

    @PostMapping("/staff")
    public ApiResponse<com.gis.logistics.domain.staff.StaffProfile> addStaff(@RequestBody com.gis.logistics.domain.staff.StaffService.AddStaffCommand cmd) {
        return ApiResponse.ok(staffService.add(cmd));
    }

    @GetMapping("/staff")
    public ApiResponse<java.util.List<com.gis.logistics.domain.staff.StaffProfile>> staff(@RequestParam(required = false) Long siteId) {
        return ApiResponse.ok(siteId == null ? staffService.byStatus(1) : staffService.bySite(siteId));
    }

    @PatchMapping("/staff/{id}/status")
    public ApiResponse<com.gis.logistics.domain.staff.StaffProfile> setStaffStatus(@PathVariable Long id,
                                                                                   @RequestBody @jakarta.validation.Valid UserStatusBody body) {
        return ApiResponse.ok(staffService.setStatus(id, body.status()));
    }

    @PatchMapping("/staff/{id}/license")
    public ApiResponse<com.gis.logistics.domain.staff.StaffProfile> setStaffLicense(@PathVariable Long id,
                                                                                    @RequestBody LicenseBody body) {
        return ApiResponse.ok(staffService.updateLicense(id, body.licenseNo()));
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<Order> adminTransition(@PathVariable Long id, @RequestBody @jakarta.validation.Valid OrderStatusBody body) {
        return ApiResponse.ok(orderService.transition(id, body.status(), 0L));
    }

    public record UserStatusBody(@PositiveOrZero Integer status) {}
    public record AppRejectBody(String reason) {}
    @GetMapping("/reviews")
    public ApiResponse<java.util.List<com.gis.logistics.domain.review.Review>> reviews(@RequestParam(required = false) Long staffId) {
        return ApiResponse.ok(staffId == null
                ? reviewRepository.findAll()
                : reviewRepository.findByStaffId(staffId, org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
    }

    @PostMapping("/reviews/{id}/delete")
    public ApiResponse<com.gis.logistics.domain.review.Review> deleteReview(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.delete(id));
    }

    @GetMapping("/reviews/approval-rate")
    public ApiResponse<com.gis.logistics.domain.review.ReviewService.ApprovalRate> approvalRate() {
        return ApiResponse.ok(reviewService.approvalRate());
    }

    @GetMapping("/feedback")
    public ApiResponse<java.util.List<com.gis.logistics.domain.feedback.Feedback>> feedback(@RequestParam(required = false) com.gis.logistics.domain.feedback.Feedback.Status status) {
        var p = feedbackService.all(org.springframework.data.domain.PageRequest.of(0, 100));
        return ApiResponse.ok(p.getContent());
    }

    @PostMapping("/feedback/{id}/process")
    public ApiResponse<com.gis.logistics.domain.feedback.Feedback> processFeedback(@PathVariable Long id,
                                                                                  @RequestBody @jakarta.validation.Valid FeedbackStatusBody body) {
        return ApiResponse.ok(feedbackService.process(id, body.status()));
    }

    @PostMapping("/feedback/{id}/reply")
    public ApiResponse<com.gis.logistics.domain.feedback.Feedback> replyFeedback(@PathVariable Long id,
                                                                                 @RequestBody ReplyBody body) {
        return ApiResponse.ok(feedbackService.reply(id, body.text()));
    }

    @GetMapping("/im/sessions")
    public ApiResponse<java.util.List<com.gis.logistics.domain.im.ImSession>> imSessions() {
        return ApiResponse.ok(imService.activeSessions());
    }

    @PostMapping("/im/sessions/{id}/assign")
    public ApiResponse<com.gis.logistics.domain.im.ImSession> imAssign(@PathVariable Long id, @RequestParam Long adminId) {
        return ApiResponse.ok(imService.assign(id, adminId));
    }

    @PostMapping("/im/sessions/{id}/close")
    public ApiResponse<com.gis.logistics.domain.im.ImSession> imClose(@PathVariable Long id) {
        return ApiResponse.ok(imService.close(id));
    }

    @GetMapping("/im/sessions/{id}/messages")
    public ApiResponse<java.util.List<com.gis.logistics.domain.im.ImMessage>> imMessages(@PathVariable Long id) {
        return ApiResponse.ok(imService.history(id));
    }

    @GetMapping("/warehouse/sites")
    public ApiResponse<java.util.List<com.gis.logistics.domain.warehouse.Site>> sites() {
        return ApiResponse.ok(warehouseService.activeSites());
    }

    @PostMapping("/warehouse/sites")
    public ApiResponse<com.gis.logistics.domain.warehouse.Site> addSite(@RequestBody @jakarta.validation.Valid SiteBody body) {
        return ApiResponse.ok(warehouseService.addSite(body.code(), body.name(), body.capacity()));
    }

    @GetMapping("/warehouse/{siteId}/stock")
    public ApiResponse<java.lang.Long> stock(@PathVariable Long siteId) {
        return ApiResponse.ok(warehouseService.stockOf(siteId));
    }

    @GetMapping("/warehouse/{siteId}/records")
    public ApiResponse<java.util.List<com.gis.logistics.domain.warehouse.WarehouseRecord>> warehouseRecords(@PathVariable Long siteId) {
        return ApiResponse.ok(warehouseService.recordsOf(siteId));
    }

    @PostMapping("/warehouse/{siteId}/inbound")
    public ApiResponse<com.gis.logistics.domain.warehouse.WarehouseRecord> inbound(@PathVariable Long siteId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) @jakarta.validation.Valid WhBody body,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long orderId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer quantity) {
        int qty = quantity != null ? quantity : (body != null && body.quantity() != null ? body.quantity() : 1);
        Long oid = orderId != null ? orderId : (body != null ? body.orderId() : null);
        return ApiResponse.ok(oid == null ? warehouseService.manualInbound(siteId, qty)
                : warehouseService.inbound(siteId, oid, qty));
    }

    @PostMapping("/warehouse/{siteId}/outbound")
    public ApiResponse<com.gis.logistics.domain.warehouse.WarehouseRecord> outbound(@PathVariable Long siteId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) @jakarta.validation.Valid WhBody body,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long orderId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer quantity) {
        int qty = quantity != null ? quantity : (body != null && body.quantity() != null ? body.quantity() : 1);
        Long oid = orderId != null ? orderId : (body != null ? body.orderId() : null);
        return ApiResponse.ok(oid == null ? warehouseService.manualOutbound(siteId, qty)
                : warehouseService.outbound(siteId, oid, qty));
    }

    @GetMapping("/demands/audit")
    public ApiResponse<org.springframework.data.domain.Page<com.gis.logistics.domain.demand.Demand>> demandAudit(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(demandService.auditPending(org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @PostMapping("/demands/{id}/close")
    public ApiResponse<com.gis.logistics.domain.demand.Demand> closeDemand(@PathVariable Long id,
                                                                            @RequestParam(required = false) String reason) {
        return ApiResponse.ok(demandService.close(id, reason));
    }

    @GetMapping("/routes")
    public ApiResponse<RouteStatsService.RouteSnapshot> routes(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(routeStatsService.snapshot(Math.max(1, Math.min(50, limit))));
    }

    @GetMapping("/profile")
    public ApiResponse<java.util.Map<String, Object>> adminProfile(org.springframework.security.core.Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_NOT_FOUND, "admin not found"));
        String phonePlain;
        try {
            String phoneB64 = u.getPhoneEnc() == null ? "" : java.util.Base64.getEncoder().encodeToString(u.getPhoneEnc());
            phonePlain = phoneCipher.decrypt(phoneB64);
        } catch (Exception e) {
            phonePlain = "";
        }
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("role", u.getRole().name());
        m.put("phoneMasked", com.gis.logistics.common.crypto.PhoneMasker.mask(phonePlain));
        m.put("createdAt", u.getCreatedAt());
        return ApiResponse.ok(m);
    }

    @PatchMapping("/profile")
    public ApiResponse<java.util.Map<String, Object>> updateAdminProfile(@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid AdminProfileBody body,
                                                                          org.springframework.security.core.Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_BAD_REQUEST, "admin not found"));
        if (body.name() != null && !body.name().isBlank()) {
            u.setName(body.name().trim());
        }
        User saved = userRepository.save(u);
        return ApiResponse.ok(java.util.Map.of("id", saved.getId(), "name", saved.getName()));
    }

    @PostMapping("/password")
    public ApiResponse<java.util.Map<String, Object>> changeAdminPassword(@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid AdminPasswordBody body,
                                                                           org.springframework.security.core.Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = userRepository.findById(userId).orElseThrow();
        String oldHash = u.getPasswordHash();
        if (oldHash == null || !passwordEncoder.matches(body.oldPassword(), oldHash)) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.AUTH_TOKEN_INVALID, "old password mismatch");
        }
        u.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        userRepository.save(u);
        return ApiResponse.ok(java.util.Map.of("changed", true));
    }

    public record OrderStatusBody(OrderStatus status) {}
    public record AdminProfileBody(String name) {}
    public record AdminPasswordBody(@jakarta.validation.constraints.NotBlank String oldPassword,
                                    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 6, max = 64) String newPassword) {}

    public record SiteBody(@jakarta.validation.constraints.NotBlank String code,
                           @jakarta.validation.constraints.NotBlank String name,
                           @jakarta.validation.constraints.Positive int capacity) {}
    public record WhBody(@jakarta.validation.constraints.Positive Long orderId /* 可空：手动出入库可不关联订单 */,
                @jakarta.validation.constraints.Positive Integer quantity) {}
    public record FeedbackStatusBody(com.gis.logistics.domain.feedback.Feedback.Status status) {}
    public record ReplyBody(@jakarta.validation.constraints.NotBlank String text) {}
    public record LicenseBody(@jakarta.validation.constraints.NotBlank String licenseNo) {}
}











