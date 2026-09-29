-- V9__reviews_rating_to_int.sql
-- reviews.rating TINYINT -> INT，与实体 Integer 对齐
ALTER TABLE reviews
    MODIFY COLUMN rating INT NOT NULL DEFAULT 5;