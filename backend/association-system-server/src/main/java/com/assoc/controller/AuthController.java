package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.dto.ChangePasswordDTO;
import com.assoc.dto.LoginDTO;
import com.assoc.dto.ProfileUpdateDTO;
import com.assoc.service.AuthService;
import com.assoc.vo.LoginVO;
import com.assoc.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与个人中心（04 文档 §4.1）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录：返回 token 与用户信息（含角色） */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResponse.ok(authService.login(dto));
    }

    /** 退出（前端清除 Token） */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    /** 修改本人密码（原密码、新密码） */
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return ApiResponse.ok();
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(authService.me());
    }

    /** 维护个人资料（姓名、手机号、头像） */
    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return ApiResponse.ok(authService.updateProfile(dto));
    }
}
