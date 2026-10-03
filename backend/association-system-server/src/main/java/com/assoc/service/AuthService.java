package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.dto.ChangePasswordDTO;
import com.assoc.dto.LoginDTO;
import com.assoc.dto.ProfileUpdateDTO;
import com.assoc.entity.SysUser;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.JwtUtil;
import com.assoc.security.UserContext;
import com.assoc.vo.LoginVO;
import com.assoc.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证与个人资料（04 文档 §4.1）。
 * 密码 BCrypt 校验；修改密码后由前端退出重新登录（02 文档 §5.1 简单方案）。
 */
@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final OperationLogService operationLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper, JwtUtil jwtUtil, OperationLogService operationLogService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
        this.operationLogService = operationLogService;
    }

    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.username()));
        if (user == null) {
            operationLogService.record(null, Constants.MODULE_AUTH, "登录失败", "账号不存在: " + dto.username());
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != Constants.STATUS_ENABLED) {
            operationLogService.record(user.getId(), Constants.MODULE_AUTH, "登录失败", "账号已停用");
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            operationLogService.record(user.getId(), Constants.MODULE_AUTH, "登录失败", "密码错误");
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }
        String token = jwtUtil.issue(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
        operationLogService.record(user.getId(), Constants.MODULE_AUTH, "登录成功", "角色: " + user.getRole());
        return new LoginVO(token, toUserVO(user));
    }

    public UserVO me() {
        SysUser user = requireUser(UserContext.userId());
        return toUserVO(user);
    }

    /** 维护个人资料（姓名、手机号、头像） */
    public UserVO updateProfile(ProfileUpdateDTO dto) {
        Long userId = UserContext.userId();
        SysUser user = requireUser(userId);
        user.setRealName(dto.realName());
        user.setPhone(dto.phone());
        user.setAvatar(dto.avatar());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        operationLogService.record(userId, Constants.MODULE_AUTH, "维护个人资料", null);
        return toUserVO(user);
    }

    public void changePassword(ChangePasswordDTO dto) {
        Long userId = UserContext.userId();
        SysUser user = requireUser(userId);
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        // 02 文档 §5.1：本期采用“修改密码即退出重新登录”，前端负责清除 Token
        operationLogService.record(userId, Constants.MODULE_AUTH, "修改密码", null);
    }

    public void logout() {
        operationLogService.recordCurrent(Constants.MODULE_AUTH, "退出登录", null);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在");
        }
        return user;
    }

    public static UserVO toUserVO(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(),
                user.getRole(), user.getAvatar(), user.getStatus(), user.getCreatedAt());
    }
}
