package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.security.RequireRole;
import com.assoc.service.ActivityService;
import com.assoc.vo.MySignupVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端：我的报名（04 文档 §4.2）。
 */
@RestController
@RequestMapping("/api/me")
public class SignupController {

    private final ActivityService activityService;

    public SignupController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** 我的报名（状态：已报名/候补/已取消） */
    @RequireRole(Constants.ROLE_STUDENT)
    @GetMapping("/signups")
    public ApiResponse<PageResult<MySignupVO>> mySignups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(activityService.mySignups(status, page, size));
    }
}
