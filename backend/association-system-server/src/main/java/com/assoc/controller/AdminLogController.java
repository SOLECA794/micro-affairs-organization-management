package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.security.RequireRole;
import com.assoc.service.AdminLogService;
import com.assoc.vo.LogVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理端：操作日志查询（04 文档 §4.4）。
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequireRole(Constants.ROLE_ADMIN)
public class AdminLogController {

    private final AdminLogService adminLogService;

    public AdminLogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    /** 日志分页查询：按模块、关键词、时间段筛选 */
    @GetMapping
    public ApiResponse<PageResult<LogVO>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(adminLogService.page(module, keyword, startTime, endTime, page, size));
    }
}
