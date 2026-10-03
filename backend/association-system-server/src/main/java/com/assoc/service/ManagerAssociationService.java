package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.dto.ManagerAssociationUpdateDTO;
import com.assoc.entity.Association;
import com.assoc.entity.SysUser;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.AssociationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 社团端：本社团资料查看与维护（负责人仅能维护绑定社团）。
 */
@Service
public class ManagerAssociationService {

    private final AssociationMapper associationMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

    public ManagerAssociationService(AssociationMapper associationMapper, SysUserMapper sysUserMapper,
                                     OperationLogService operationLogService) {
        this.associationMapper = associationMapper;
        this.sysUserMapper = sysUserMapper;
        this.operationLogService = operationLogService;
    }

    public AssociationVO getOwn() {
        Long managerId = UserContext.userId();
        Association association = associationMapper.selectOne(new LambdaQueryWrapper<Association>()
                .eq(Association::getLeaderUserId, managerId)
                .last("LIMIT 1"));
        if (association == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定社团");
        }
        return toVO(association);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssociationVO updateOwn(ManagerAssociationUpdateDTO dto) {
        Long managerId = UserContext.userId();
        Association association = associationMapper.selectOne(new LambdaQueryWrapper<Association>()
                .eq(Association::getLeaderUserId, managerId)
                .last("LIMIT 1"));
        if (association == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定社团");
        }
        association.setName(dto.name());
        association.setCategory(dto.category());
        association.setDescription(dto.description());
        association.setUpdatedAt(LocalDateTime.now());
        associationMapper.updateById(association);
        operationLogService.record(managerId, Constants.MODULE_ADMIN, "维护社团资料",
                "社团: " + association.getName());
        return toVO(association);
    }

    private AssociationVO toVO(Association association) {
        SysUser leader = sysUserMapper.selectById(association.getLeaderUserId());
        return new AssociationVO(association.getId(), association.getName(), association.getCode(),
                association.getCategory(), association.getLeaderUserId(),
                leader == null ? null : leader.getRealName(),
                association.getStatus(), association.getDescription(), association.getCreatedAt());
    }
}
