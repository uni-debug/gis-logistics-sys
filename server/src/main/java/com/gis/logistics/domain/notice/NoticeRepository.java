package com.gis.logistics.domain.notice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE n.status = 1 ORDER BY n.pinned DESC, n.id DESC")
    List<Notice> findPublishedPinnedFirst(Pageable pageable);

    Page<Notice> findByStatus(Integer status, Pageable pageable);
}
