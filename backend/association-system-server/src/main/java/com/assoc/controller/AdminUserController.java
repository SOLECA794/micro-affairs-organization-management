package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.dto.UserStatusDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.AdminUserService;
import com.assoc.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：用户管理（04 文档 §4.4）。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequireRole(Constants.ROLE_ADMIN)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /** 用户列表（分页、筛选） */
    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(adminUserService.page(keyword, role, status, page, size));
    }

    /** 启用/停用用户 */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        adminUserService.updateStatus(id, dto.status());
        return ApiResponse.ok();
    }
}
