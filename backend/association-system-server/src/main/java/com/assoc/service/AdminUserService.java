package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.dto.UserCreateDTO;
import com.assoc.security.UserContext;
import com.assoc.entity.SysUser;
import com.assoc.mapper.SysUserMapper;
import com.assoc.vo.UserPasswordVO;
import com.assoc.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端：用户列表、启停用、创建用户、重置密码（越权控制：仅 ADMIN）。
 */
@Service
public class AdminUserService {

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 8;

    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AdminUserService(SysUserMapper sysUserMapper, OperationLogService operationLogService) {
        this.sysUserMapper = sysUserMapper;
        this.operationLogService = operationLogService;
    }

    /** 用户列表（分页、按关键词/角色/状态筛选） */
    public PageResult<UserVO> page(String keyword, String role, Integer status, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .eq(role != null && !role.isBlank(), SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserVO> list = result.getRecords().stream().map(AuthService::toUserVO).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 启用/停用用户 */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        if (status == null || (status != Constants.STATUS_ENABLED && status != Constants.STATUS_DISABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态取值不合法");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN,
                status == Constants.STATUS_ENABLED ? "启用用户" : "停用用户",
                "用户: " + user.getUsername());
    }

    /** 创建用户（实现期扩展）：username 唯一，BCrypt 落库，初始密码明文仅返回一次 */
    @Transactional(rollbackFor = Exception.class)
    public UserPasswordVO create(UserCreateDTO dto) {
        SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.username()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.DUPLICATE, "用户名已存在: " + dto.username());
        }
        String rawPassword = dto.password() != null && !dto.password().isBlank()
                ? dto.password().trim() : generatePassword();
        if (rawPassword.length() < 8 || rawPassword.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度需为 8-64 位");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(dto.realName());
        user.setPhone(dto.phone());
        user.setRole(dto.role());
        user.setStatus(Constants.STATUS_ENABLED);
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUPLICATE, "用户名已存在: " + dto.username());
        }
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "创建用户",
                "用户: " + user.getUsername() + "，角色: " + user.getRole());
        return new UserPasswordVO(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), rawPassword);
    }

    /** 重置密码（实现期扩展）：服务端生成或接收指定密码，BCrypt 落库，明文仅返回一次 */
    @Transactional(rollbackFor = Exception.class)
    public UserPasswordVO resetPassword(Long userId, String password) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String rawPassword = password != null && !password.isBlank() ? password.trim() : generatePassword();
        if (rawPassword.length() < 8 || rawPassword.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度需为 8-64 位");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "重置密码",
                "用户: " + user.getUsername());
        return new UserPasswordVO(user.getId(), user.getUsername(), user.getRealName(),
                user.getRole(), rawPassword);
    }

    /** 随机初始密码：去除易混淆字符（0/O/1/l/I），SecureRandom 生成 */
    private String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
