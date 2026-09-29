package com.gis.logistics.domain.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByStatus(Feedback.Status status, Pageable pageable);

    List<Feedback> findByUserIdAndType(Long userId, Feedback.Type type);

    List<Feedback> findByUserId(Long userId);

    long countByStatus(Feedback.Status status);
}
