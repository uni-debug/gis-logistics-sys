package com.gis.logistics.domain.notice;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 公告管理：创建/发布/置顶/归档/删除。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public Notice create(String title, String body) {
        if (title == null || title.isBlank()) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "notice title required");
        }
        Notice n = new Notice();
        n.setTitle(title.trim());
        n.setBody(body == null ? "" : body);
        n.setStatus(Notice.Status.DRAFT.ordinal());
        n.setPinned(false);
        return noticeRepository.save(n);
    }

    @Transactional
    public Notice publish(Long id) {
        Notice n = get(id);
        n.setStatus(Notice.Status.PUBLISHED.ordinal());
        return noticeRepository.save(n);
    }

    @Transactional
    public Notice archive(Long id) {
        Notice n = get(id);
        n.setStatus(Notice.Status.ARCHIVED.ordinal());
        return noticeRepository.save(n);
    }

    @Transactional
    public Notice pin(Long id, boolean pinned) {
        Notice n = get(id);
        n.setPinned(pinned);
        return noticeRepository.save(n);
    }

    public List<Notice> published() {
        return noticeRepository.findPublishedPinnedFirst(org.springframework.data.domain.PageRequest.of(0, 50));
    }

    public Notice get(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "notice not found: " + id));
    }
}






