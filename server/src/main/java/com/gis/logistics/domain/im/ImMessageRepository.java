package com.gis.logistics.domain.im;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImMessageRepository extends JpaRepository<ImMessage, Long> {
    List<ImMessage> findBySessionIdOrderBySentAtAsc(Long sessionId);
    Page<ImMessage> findBySessionId(Long sessionId, Pageable pageable);
}
