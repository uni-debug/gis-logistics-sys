package com.gis.logistics.domain.order;

import com.gis.logistics.domain.admin.MonthAggregation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.staffId = :staffId AND (:status IS NULL OR o.status = :status)")
    Page<Order> findByStaffIdAndStatus(@Param("staffId") Long staffId, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) " +
            "AND (:staffId IS NULL OR o.staffId = :staffId) " +
            "AND (:userId IS NULL OR o.userId = :userId) " +
            "AND (:from IS NULL OR o.createdAt >= CAST(:from AS timestamp)) " +
            "AND (:to IS NULL OR o.createdAt <= CAST(:to AS timestamp))")
    Page<Order> findByFilter(@Param("status") OrderStatus status,
                             @Param("staffId") Long staffId,
                             @Param("userId") Long userId,
                             @Param("from") String from,
                             @Param("to") String to,
                             Pageable pageable);

    List<Order> findByStaffId(Long staffId);

    long countByStaffIdAndStatus(Long staffId, OrderStatus status);

    long countByStaffId(Long staffId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByStatusAndStaffId(OrderStatus status, Long staffId, org.springframework.data.domain.Pageable pageable);

    Page<Order> findByStatusIsNull(Pageable pageable);

    @Query("SELECT FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') AS ym, " +
            "COUNT(o.id) AS orderCount, COALESCE(SUM(o.amount), 0) AS totalFen " +
            "FROM Order o WHERE o.createdAt IS NOT NULL " +
            "GROUP BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') ORDER BY ym")
    List<MonthAggregation> aggregateMonthly();

}
