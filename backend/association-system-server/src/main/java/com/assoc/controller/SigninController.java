package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.entity.Attendance;
import com.assoc.dto.SigninScanDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.SigninService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端：扫码签到（04 文档 §4.2）。GET /api/signin/qrcode 为扫码落地页（公开，重定向到前端签到页）。
 */
@RestController
@RequestMapping("/api/signin")
public class SigninController {

    private final SigninService signinService;

    public SigninController(SigninService signinService) {
        this.signinService = signinService;
    }

    /** 扫码签到：提交 {activityId, token} */
    @RequireRole(Constants.ROLE_STUDENT)
    @PostMapping("/qrcode")
    public ApiResponse<Attendance> scan(@Valid @RequestBody SigninScanDTO dto) {
        return ApiResponse.ok(signinService.scan(dto.activityId(), dto.token()));
    }
}
