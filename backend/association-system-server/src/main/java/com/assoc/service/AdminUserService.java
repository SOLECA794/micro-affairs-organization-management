package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.security.UserContext;
import com.assoc.entity.SysUser;
import com.assoc.mapper.SysUserMapper;
import com.assoc.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端：用户列表与启停用（越权控制：仅 ADMIN）。
 */
@Service
public class AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

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
}
