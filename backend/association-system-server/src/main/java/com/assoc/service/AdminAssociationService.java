package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.dto.AssociationCreateDTO;
import com.assoc.dto.AssociationUpdateDTO;
import com.assoc.entity.Association;
import com.assoc.entity.SysUser;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.AssociationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端：社团管理（创建、维护，含负责人与状态）。
 */
@Service
public class AdminAssociationService {

    private final AssociationMapper associationMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

    public AdminAssociationService(AssociationMapper associationMapper, SysUserMapper sysUserMapper,
                                   OperationLogService operationLogService) {
        this.associationMapper = associationMapper;
        this.sysUserMapper = sysUserMapper;
        this.operationLogService = operationLogService;
    }

    /** 社团列表（分页、关键词） */
    public PageResult<AssociationVO> page(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<Association> wrapper = new LambdaQueryWrapper<Association>()
                .like(keyword != null && !keyword.isBlank(), Association::getName, keyword)
                .eq(status != null, Association::getStatus, status)
                .orderByDesc(Association::getCreatedAt);
        Page<Association> result = associationMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, SysUser> leaders = result.getRecords().stream().map(Association::getLeaderUserId).distinct()
                .map(sysUserMapper::selectById).filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<AssociationVO> list = result.getRecords().stream()
                .map(assoc -> toVO(assoc, leaders.get(assoc.getLeaderUserId()))).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 创建社团并绑定负责人 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AssociationCreateDTO dto) {
        requireManager(dto.leaderUserId());
        checkLeaderNotBound(dto.leaderUserId(), null);
        Association association = new Association();
        association.setName(dto.name());
        association.setCode(dto.code());
        association.setCategory(dto.category());
        association.setLeaderUserId(dto.leaderUserId());
        association.setStatus(Constants.STATUS_ENABLED);
        association.setDescription(dto.description());
        association.setDeleted(0);
        association.setCreatedAt(LocalDateTime.now());
        association.setUpdatedAt(LocalDateTime.now());
        try {
            associationMapper.insert(association);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "社团编号已存在");
        }
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "创建社团",
                "社团: " + association.getName());
        return association.getId();
    }

    /** 维护社团（名称、分类、负责人、状态、简介） */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AssociationUpdateDTO dto) {
        Association association = associationMapper.selectById(id);
        if (association == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "社团不存在");
        }
        requireManager(dto.leaderUserId());
        checkLeaderNotBound(dto.leaderUserId(), id);
        if (dto.status() != Constants.STATUS_ENABLED && dto.status() != Constants.STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态取值不合法");
        }
        association.setName(dto.name());
        association.setCategory(dto.category());
        association.setLeaderUserId(dto.leaderUserId());
        association.setStatus(dto.status());
        association.setDescription(dto.description());
        association.setUpdatedAt(LocalDateTime.now());
        associationMapper.updateById(association);
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "维护社团",
                "社团: " + association.getName());
    }

    private void requireManager(Long userId) {
        SysUser leader = sysUserMapper.selectById(userId);
        if (leader == null || !Constants.ROLE_MANAGER.equals(leader.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "负责人必须为社团负责人角色账号");
        }
    }

    /**
     * 一个负责人只能绑定一个社团：已绑定其他社团的 MANAGER 再被绑定会产生"孤儿社团"（无人管理）。
     * excludeAssociationId 用于更新场景（排除自己当前绑定的社团）。
     */
    private void checkLeaderNotBound(Long userId, Long excludeAssociationId) {
        Association existing = associationMapper.selectOne(new LambdaQueryWrapper<Association>()
                .eq(Association::getLeaderUserId, userId)
                .ne(excludeAssociationId != null, Association::getId, excludeAssociationId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该负责人已绑定社团「" + existing.getName() + "」，一个负责人只能管理一个社团");
        }
    }

    private AssociationVO toVO(Association association, SysUser leader) {
        return new AssociationVO(association.getId(), association.getName(), association.getCode(),
                association.getCategory(), association.getLeaderUserId(),
                leader == null ? null : leader.getRealName(),
                association.getStatus(), association.getDescription(), association.getCreatedAt());
    }
}
