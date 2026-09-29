package com.gis.logistics.domain.staff;

import com.gis.logistics.common.web.ApiResponse;
import com.gis.logistics.domain.demand.Demand;
import com.gis.logistics.domain.demand.DemandRepository;
import com.gis.logistics.domain.demand.DemandService;
import com.gis.logistics.domain.logistics.LogisticsEvent;
import com.gis.logistics.domain.logistics.LogisticsEventRepository;
import com.gis.logistics.domain.review.Review;
import com.gis.logistics.domain.review.ReviewRepository;
import com.gis.logistics.domain.review.ReviewService;
import com.gis.logistics.domain.logistics.LogisticsEventService;
import com.gis.logistics.domain.order.Order;
import com.gis.logistics.domain.order.OrderRepository;
import com.gis.logistics.domain.warehouse.Site;
import com.gis.logistics.domain.warehouse.SiteRepository;
import com.gis.logistics.domain.order.OrderService;
import com.gis.logistics.domain.order.OrderStatus;
import com.gis.logistics.gis.route.RoutePlanService;
import com.gis.logistics.gis.route.RouteStrategy;
import com.gis.logistics.gis.route.RouteTask;
import com.gis.logistics.gis.route.RouteTaskRepository;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffProfileRepository staffProfileRepository;
    private final DemandService demandService;
    private final DemandRepository demandRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RoutePlanService routePlanService;
    private final RouteTaskRepository routeTaskRepository;
    private final LogisticsEventService logisticsEventService;
    private final LogisticsEventRepository logisticsEventRepository;
    private final com.gis.logistics.domain.logistics.TrackStreamService trackStreamService;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final SiteRepository siteRepository;
    private final com.gis.logistics.domain.user.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/demands")
    public ApiResponse<Page<Demand>> openDemands(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(demandService.pageByStatus(Demand.Status.PENDING, PageRequest.of(page, size)));
    }

    @PostMapping("/demands/{id}/quote")
    public ApiResponse<Demand> quote(@PathVariable Long id, @RequestBody @jakarta.validation.Valid QuoteBody body,
                                     Authentication auth) {
        Long staffProfileId = resolveStaffProfileId(auth);
        Demand d = demandService.quote(id, staffProfileId, body.priceFen());
        return ApiResponse.ok(d);
    }

    @GetMapping("/orders")
    public ApiResponse<Page<Order>> myOrders(@RequestParam(required = false) OrderStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        return ApiResponse.ok(orderRepository.findByStaffIdAndStatus(staffId, status, PageRequest.of(page, size)));
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<Order> transition(@PathVariable Long id, @RequestBody StatusBody body,
                                         Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Order o = orderService.transition(id, body.status(), staffId);
        return ApiResponse.ok(o);
    }

    @GetMapping("/orders/{id}/regions")
    public ApiResponse<java.util.Map<String, String>> orderRegions(@PathVariable Long id) {
        var m = new java.util.LinkedHashMap<String, String>();
        var o = orderRepository.findById(id).orElse(null);
        if (o != null && o.getDemandId() != null) {
            demandRepository.findById(o.getDemandId()).ifPresent(d -> {
                m.put("originRegion", d.getOriginRegion() == null ? "" : d.getOriginRegion());
                m.put("targetRegion", d.getTargetRegion() == null ? "" : d.getTargetRegion());
                m.put("originAddr", d.getOriginAddr() == null ? "" : d.getOriginAddr());
                m.put("targetAddr", d.getTargetAddr() == null ? "" : d.getTargetAddr());
            });
        }
        m.putIfAbsent("originRegion", "");
        m.putIfAbsent("targetRegion", "");
        m.putIfAbsent("originAddr", "");
        m.putIfAbsent("targetAddr", "");
        return ApiResponse.ok(m);
    }

    @PostMapping("/orders/{id}/route")
    public ApiResponse<RouteTask> planRoute(@PathVariable Long id, @RequestBody @jakarta.validation.Valid PlanRouteBody body,
                                            Authentication auth) {
        Long staffId = (Long) auth.getPrincipal();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        double fromLon = body.fromLon() != null ? body.fromLon() : require(order.getFromLon(), "fromLon");
        double fromLat = body.fromLat() != null ? body.fromLat() : require(order.getFromLat(), "fromLat");
        double toLon   = body.toLon()   != null ? body.toLon()   : require(order.getToLon(),   "toLon");
        double toLat   = body.toLat()   != null ? body.toLat()   : require(order.getToLat(),   "toLat");
        RouteTask task = routePlanService.plan(id, body.strategy(),
                fromLon, fromLat, toLon, toLat,
                body.via(), body.avoid());
        return ApiResponse.ok(task);
    }

    private double require(java.math.BigDecimal v, String name) {
        if (v == null) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.POINT_INVALID, name + " required");
        }
        return v.doubleValue();
    }

    @PatchMapping("/orders/{id}/endpoints")
    public ApiResponse<Order> setEndpoints(@PathVariable Long id,
                                            @RequestBody @jakarta.validation.Valid EndpointsBody body,
                                            Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        if (!staffId.equals(o.getStaffId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        o.setFromLon(body.fromLon() == null ? null : new java.math.BigDecimal(body.fromLon().toString()));
        o.setFromLat(body.fromLat() == null ? null : new java.math.BigDecimal(body.fromLat().toString()));
        o.setToLon(body.toLon() == null ? null : new java.math.BigDecimal(body.toLon().toString()));
        o.setToLat(body.toLat() == null ? null : new java.math.BigDecimal(body.toLat().toString()));
        return ApiResponse.ok(orderRepository.save(o));
    }

    @GetMapping("/orders/{id}/route")
    public ApiResponse<RouteTask> latestRoute(@PathVariable Long id) {
        List<RouteTask> tasks = routeTaskRepository.findByOrderIdOrderByCreatedAtDesc(id);
        if (tasks.isEmpty()) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ROUTE_PLAN_FAILED, "no route yet");
        }
        return ApiResponse.ok(tasks.get(0));
    }

    @PostMapping("/orders/{id}/events")
    public ApiResponse<com.gis.logistics.domain.logistics.LogisticsEvent> publishEvent(
            @PathVariable Long id, @RequestBody @jakarta.validation.Valid EventBody body,
            Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        var event = logisticsEventService.publish(new LogisticsEventService.EventCommand(
                id, body.type(), body.lon(), body.lat(), staffId));
        return ApiResponse.ok(event);
    }

    @GetMapping("/orders/{id}/track-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter trackStream(@PathVariable Long id, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        if (!staffId.equals(o.getStaffId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        return trackStreamService.subscribe(id);
    }

    @GetMapping("/orders/{id}/events")
    public ApiResponse<List<LogisticsEvent>> orderEvents(@PathVariable Long id, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_FOUND, "order not found: " + id));
        if (!staffId.equals(o.getStaffId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your order");
        }
        return ApiResponse.ok(logisticsEventRepository.findByOrderIdOrderByOccurredAtAsc(id));
    }

    @GetMapping("/reviews")
    public ApiResponse<List<Review>> myReviews(Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Page<Review> page = reviewRepository.findByStaffId(staffId, PageRequest.of(0, 100));
        return ApiResponse.ok(page.getContent());
    }

    @PostMapping("/reviews/{id}/reply")
    public ApiResponse<Review> replyReview(@PathVariable Long id, @RequestBody ReplyBody body, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        Review r = reviewService.reply(id, body.reply());
        if (!staffId.equals(r.getStaffId())) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.ORDER_NOT_OWNED, "not your review");
        }
        return ApiResponse.ok(r);
    }

    private Long resolveStaffProfileId(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return staffProfileRepository.findByUserId(userId)
                .map(StaffProfile::getId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ROLE_FORBIDDEN, "not a staff account"));
    }
    public record QuoteBody(@Positive int priceFen) {}

    public record StatusBody(OrderStatus status) {}

    public record PlanRouteBody(RouteStrategy strategy,
                                Double fromLon, Double fromLat,
                                Double toLon, Double toLat,
                                List<double[]> via, List<String> avoid) {}

    public record EventBody(com.gis.logistics.domain.logistics.LogisticsEvent.EventType type,
                            Double lon, Double lat) {}

    public record EndpointsBody(Double fromLon, Double fromLat,
                                Double toLon, Double toLat) {}

    public record ReplyBody(String reply) {}
    public record ProfileBody(String name) {}
    public record StaffPasswordBody(@jakarta.validation.constraints.NotBlank String oldPassword, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 6, max = 64) String newPassword) {}
    public record LicenseBody(@jakarta.validation.constraints.NotBlank String licenseNo) {}

    @GetMapping("/profile")
    public ApiResponse<StaffProfileView> profile(Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        StaffProfile sp = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ROLE_FORBIDDEN, "not a staff account"));
        Long userId = sp.getUserId();
        com.gis.logistics.domain.user.User user = userRepository.findById(userId).orElse(null);
        Site site = siteRepository.findById(sp.getSiteId()).orElse(null);

        long total = orderRepository.countByStaffId(staffId);
        long delivered = orderRepository.countByStaffIdAndStatus(staffId, OrderStatus.DELIVERED);
        long inTransit = orderRepository.countByStaffIdAndStatus(staffId, OrderStatus.IN_TRANSIT);
        Double avg = reviewRepository.averageRatingByStaff(staffId);

        StaffProfileView view = new StaffProfileView(
                userId,
                user != null ? user.getName() : null,
                null,
                sp.getLicenseNo(),
                site != null ? site.getName() : null,
                total,
                delivered,
                inTransit,
                avg);
        return ApiResponse.ok(view);
    }

    @PatchMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody @jakarta.validation.Valid ProfileBody body, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        StaffProfile sp = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ROLE_FORBIDDEN, "not a staff account"));
        if (body.name() != null && !body.name().isBlank()) {
            userRepository.findById(sp.getUserId()).ifPresent(u -> {
                u.setName(body.name().trim());
                userRepository.save(u);
            });
        }
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("userId", sp.getUserId());
        m.put("name", body.name() == null ? "unchanged" : body.name().trim());
        return ApiResponse.ok(m);
    }

    @PostMapping("/password")
    public ApiResponse<Map<String, Object>> changePassword(@RequestBody @jakarta.validation.Valid StaffPasswordBody body, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        StaffProfile sp = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ROLE_FORBIDDEN, "not a staff account"));
        com.gis.logistics.domain.user.User u = userRepository.findById(sp.getUserId())
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.GENERIC_NOT_FOUND, "user not found"));
        String oldHash = u.getPasswordHash();
        if (oldHash == null || !passwordEncoder.matches(body.oldPassword(), oldHash)) {
            throw new com.gis.logistics.common.exception.BizException(
                    com.gis.logistics.common.errorcode.ErrorCode.AUTH_TOKEN_INVALID, "old password incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        userRepository.save(u);
        return ApiResponse.ok(java.util.Map.of("changed", true));
    }

    @PatchMapping("/profile/license")
    public ApiResponse<Map<String, String>> updateLicense(@RequestBody @jakarta.validation.Valid LicenseBody body, Authentication auth) {
        Long staffId = resolveStaffProfileId(auth);
        StaffProfile sp = staffProfileRepository.findById(staffId)
                .orElseThrow(() -> new com.gis.logistics.common.exception.BizException(
                        com.gis.logistics.common.errorcode.ErrorCode.ROLE_FORBIDDEN, "not a staff account"));
        sp.setLicenseNo(body.licenseNo());
        staffProfileRepository.save(sp);
        return ApiResponse.ok(java.util.Map.of("licenseNo", sp.getLicenseNo()));
    }

    public record StaffProfileView(
            Long userId, String name, String phoneMask, String licenseNo, String siteName,
            long totalOrders, long delivered, long inTransit, Double avgRating) {}
}











