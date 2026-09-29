package com.gis.logistics.domain.im;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImSessionRepository extends JpaRepository<ImSession, Long> {
    Optional<ImSession> findFirstByUserIdAndStatus(Long userId, ImSession.Status status);
    Page<ImSession> findByStatus(ImSession.Status status, Pageable pageable);
    List<ImSession> findByAdminId(Long adminId);
}
