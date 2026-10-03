package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.security.UserContext;
import com.assoc.service.NotificationService;
import com.assoc.vo.NotificationPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的通知（04 文档 §4.2；实现期扩展：放开为任意登录角色，负责人/管理员也可读自己的通知）。
 * 归属校验保留：只能读/标记本人的通知（NotificationService.markRead 内校验）。
 */
@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 我的通知（分页，含未读数） */
    @GetMapping
    public ApiResponse<NotificationPageVO> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        Long userId = UserContext.userId();
        return ApiResponse.ok(NotificationPageVO.of(notificationService.page(userId, page, size),
                notificationService.unreadCount(userId)));
    }

    /** 标记通知已读 */
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContext.userId(), id);
        return ApiResponse.ok();
    }
}
