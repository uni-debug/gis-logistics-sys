package com.gis.logistics.domain.notice;

import com.gis.logistics.common.web.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeRepository noticeRepository;

    @GetMapping("/notices")
    public ApiResponse<List<Notice>> published() {
        return ApiResponse.ok(noticeService.published());
    }

    @PostMapping("/api/v1/admin/notices")
    public ApiResponse<Notice> create(@RequestBody @jakarta.validation.Valid NoticeBody body) {
        return ApiResponse.ok(noticeService.create(body.title(), body.body()));
    }

    @GetMapping("/api/v1/admin/notices")
    public ApiResponse<Page<Notice>> list(@RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(noticeRepository.findByStatus(status, PageRequest.of(page, size)));
    }

    @PostMapping("/api/v1/admin/notices/{id}/publish")
    public ApiResponse<Notice> publish(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.publish(id));
    }

    @PostMapping("/api/v1/admin/notices/{id}/archive")
    public ApiResponse<Notice> archive(@PathVariable Long id) {
        return ApiResponse.ok(noticeService.archive(id));
    }

    @PatchMapping("/api/v1/admin/notices/{id}/pin")
    public ApiResponse<Notice> pin(@PathVariable Long id, @RequestParam boolean pinned) {
        return ApiResponse.ok(noticeService.pin(id, pinned));
    }

    public record NoticeBody(@NotBlank String title, String body) {}
}
