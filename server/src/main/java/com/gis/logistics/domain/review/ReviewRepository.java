package com.gis.logistics.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByStaffId(Long staffId, Pageable pageable);

    List<Review> findByOrderId(Long orderId);

    List<Review> findByUserIdAndDeletedFalse(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.deleted = false AND r.staffId = :staffId")
    Double averageRatingByStaff(Long staffId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false")
    long countActive();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.deleted = false AND r.rating >= 4")
    long countPositiveActive();
}
