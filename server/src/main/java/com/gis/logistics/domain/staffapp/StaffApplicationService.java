package com.gis.logistics.domain.staffapp;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import com.gis.logistics.domain.staff.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 员工申请（权限关键路径）：发起 / 审批 / 拒绝。
 * 审批通过时复用 StaffService.add，自动升角色并建档案。
 */
@Service
@RequiredArgsConstructor
public class StaffApplicationService {

    private final StaffApplicationRepository repository;
    private final UserRepository userRepository;
    private final StaffService staffService;

    @Transactional
    public StaffApplication submit(Long userId, Long siteId, String licenseNo, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "user not found: " + userId));
        if (user.getRole() != User.Role.USER) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "only USER role may apply for staff");
        }
        // 一人一申请：已有待审/已拒绝/已通过均不可重复提交，仅允许删除/撤回后重新申请
        repository.findByUserId(userId).ifPresent(existing -> {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "an application already exists: " + existing.getStatus());
        });
        StaffApplication app = new StaffApplication();
        app.setUserId(userId);
        app.setSiteId(siteId);
        app.setLicenseNo(licenseNo);
        app.setReason(reason);
        app.setStatus(StaffApplication.Status.PENDING);
        // 默认配送区域占位点（delivery_area 非空）
        app.setDeliveryArea(new org.locationtech.jts.geom.GeometryFactory()
                .createPoint(new org.locationtech.jts.geom.Coordinate(116.4, 39.9)));
        return repository.save(app);
    }

    @Transactional
    public StaffApplication approve(Long applicationId, Long adminId) {
        StaffApplication app = repository.findById(applicationId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "application not found"));
        if (app.getStatus() != StaffApplication.Status.PENDING) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "application not pending");
        }
        User user = userRepository.findById(app.getUserId())
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "user not found"));
        // 复用 StaffService.add（已含 delivery_area 占位 + 角色提升）
        staffService.add(new StaffService.AddStaffCommand(app.getUserId(), app.getSiteId(), app.getLicenseNo()));
        app.setStatus(StaffApplication.Status.APPROVED);
        app.setAdminId(adminId);
        app.setDecidedAt(java.time.LocalDateTime.now());
        return repository.save(app);
    }

    @Transactional
    public StaffApplication reject(Long applicationId, Long adminId, String reason) {
        StaffApplication app = repository.findById(applicationId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "application not found"));
        if (app.getStatus() != StaffApplication.Status.PENDING) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "application not pending");
        }
        app.setStatus(StaffApplication.Status.REJECTED);
        app.setAdminId(adminId);
        app.setReason(reason);
        app.setDecidedAt(java.time.LocalDateTime.now());
        return repository.save(app);
    }

    @Transactional
    public StaffApplication cancel(Long userId) {
        StaffApplication app = repository.findByUserId(userId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "no application"));
        if (app.getStatus() != StaffApplication.Status.PENDING) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "only PENDING may be cancelled");
        }
        repository.delete(app);
        return app;
    }

    public List<StaffApplication> byStatus(StaffApplication.Status status) {
        return status == null ? repository.findAll() : repository.findByStatus(status);
    }
}
