package com.gis.logistics.domain.staff;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffProfileRepository staffRepository;
    @Mock
    private UserRepository userRepository;

    private StaffService service;

    @BeforeEach
    void setUp() {
        service = new StaffService(staffRepository, userRepository);
    }

    @Test
    void addPersistsStaff() {
        when(staffRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenAnswer(inv -> {
            User u = new User();
            u.setId(10L);
            return Optional.of(u);
        });
        when(staffRepository.save(any())).thenAnswer(inv -> {
            StaffProfile p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        StaffProfile p = service.add(new StaffService.AddStaffCommand(10L, 5L, "LIC-001"));
        assertEquals(1L, p.getId());
        assertEquals("LIC-001", p.getLicenseNo());
    }

    @Test
    void addRejectsDuplicateUser() {
        StaffProfile existing = new StaffProfile(10L, 5L, "X");
        when(staffRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        assertThrows(BizException.class,
                () -> service.add(new StaffService.AddStaffCommand(10L, 5L, "LIC-001")));
    }

    @Test
    void addRejectsUnknownUser() {
        when(staffRepository.findByUserId(999L)).thenReturn(Optional.empty());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class,
                () -> service.add(new StaffService.AddStaffCommand(999L, 5L, "LIC-001")));
        assertEquals(ErrorCode.GENERIC_NOT_FOUND, ex.getCode());
    }

    @Test
    void disableStaff() {
        StaffProfile p = new StaffProfile(10L, 5L, "LIC-001");
        when(staffRepository.findById(1L)).thenReturn(Optional.of(p));
        when(staffRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        StaffProfile after = service.setStatus(1L, 0);
        assertEquals(0, after.getStatus());
    }
}




