package com.gis.logistics.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "rating", nullable = false)
    private Integer rating = 5;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_urls", columnDefinition = "JSON")
    private String imageUrls;

    @Column(name = "staff_reply", columnDefinition = "TEXT")
    private String staffReply;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Review() {
    }

    public Review(Long orderId, Long userId, Long staffId, int rating, String content) {
        this.orderId = orderId;
        this.userId = userId;
        this.staffId = staffId;
        this.rating = rating;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
