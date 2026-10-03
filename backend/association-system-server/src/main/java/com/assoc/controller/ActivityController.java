package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.security.RequireRole;
import com.assoc.service.ActivityService;
import com.assoc.service.SignupService;
import com.assoc.service.SigninService;
import com.assoc.vo.ActivityDetailVO;
import com.assoc.vo.ActivityListVO;
import com.assoc.vo.SignupResultVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 学生端：活动列表（公开）、活动详情、报名、取消（04 文档 §4.2）。
 */
@Validated
@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService activityService;
    private final SignupService signupService;

    public ActivityController(ActivityService activityService, SignupService signupService) {
        this.activityService = activityService;
        this.signupService = signupService;
    }

    /** 活动列表（按社团、时间、状态、关键词，分页；未传 status 默认 PUBLISHED） */
    @GetMapping("/activities")
    public ApiResponse<PageResult<ActivityListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long associationId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "startTimeBegin", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTimeBegin,
            @RequestParam(name = "startTimeEnd", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTimeEnd,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(activityService.pageActivities(keyword, associationId, categoryId, status,
                startTimeBegin, startTimeEnd, page, size));
    }

    /** 活动详情（含剩余名额、报名状态） */
    @RequireRole(Constants.ROLE_STUDENT)
    @GetMapping("/activities/{id}")
    public ApiResponse<ActivityDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(id));
    }

    /** 报名（成功或进入候补） */
    @RequireRole(Constants.ROLE_STUDENT)
    @PostMapping("/activities/{id}/signup")
    public ApiResponse<SignupResultVO> signup(@PathVariable Long id) {
        return ApiResponse.ok(signupService.signup(id));
    }

    /** 取消报名（截止前） */
    @RequireRole(Constants.ROLE_STUDENT)
    @PostMapping("/activities/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        signupService.cancel(id);
        return ApiResponse.ok();
    }
}
