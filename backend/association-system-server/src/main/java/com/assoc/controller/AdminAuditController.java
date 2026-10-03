package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.dto.AuditRejectDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.AdminAuditService;
import com.assoc.vo.AuditItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：活动审核（04 文档 §4.4）。
 */
@RestController
@RequestMapping("/api/admin/audits")
@RequireRole(Constants.ROLE_ADMIN)
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    /** 待审核/全部审核活动列表（默认待审核在前） */
    @GetMapping
    public ApiResponse<PageResult<AuditItemVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(adminAuditService.page(status, keyword, page, size));
    }

    /** 审核通过：PENDING → APPROVED，并通知负责人 */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        adminAuditService.approve(id);
        return ApiResponse.ok();
    }

    /** 驳回（必填意见）：PENDING → REJECTED，并通知负责人 */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody AuditRejectDTO dto) {
        adminAuditService.reject(id, dto.comment());
        return ApiResponse.ok();
    }
}
