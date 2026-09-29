package com.gis.logistics.domain.staff;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 员工管理（权限管控关键路径，需人工复核）：添加/编辑/启停。
 * 员工账号由管理员统一创建，绑定执业资质与配送区域。
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffProfileRepository staffRepository;
    private final com.gis.logistics.domain.user.UserRepository userRepository;

    public record AddStaffCommand(Long userId, Long siteId, String licenseNo) {}

    @Transactional
    public StaffProfile add(AddStaffCommand cmd) {
        if (staffRepository.findByUserId(cmd.userId()).isPresent()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "user already staff");
        }
        com.gis.logistics.domain.user.User user = userRepository.findById(cmd.userId())
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "user not found: " + cmd.userId()));
        // 先创建员工档案（此时用户角色仍为 USER，避免自我阻断），再在事务内升角色
        StaffProfile p = new StaffProfile(cmd.userId(), cmd.siteId(), cmd.licenseNo());
        p.setDeliveryArea(new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(116.4, 39.9)));
        StaffProfile saved = staffRepository.save(p);
        user.setRole(com.gis.logistics.domain.user.User.Role.STAFF);
        userRepository.save(user);
        return saved;
    }

    @Transactional
    public StaffProfile setStatus(Long staffId, int status) {
        StaffProfile p = staffRepository.findById(staffId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "staff not found: " + staffId));
        p.setStatus(status);
        return staffRepository.save(p);
    }

    @Transactional
    public StaffProfile updateLicense(Long staffId, String licenseNo) {
        StaffProfile p = staffRepository.findById(staffId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "staff not found: " + staffId));
        p.setLicenseNo(licenseNo);
        return staffRepository.save(p);
    }

    public List<StaffProfile> bySite(Long siteId) {
        return staffRepository.findBySiteId(siteId);
    }

    public List<StaffProfile> byStatus(int status) {
        return staffRepository.findByStatus(status);
    }
}









