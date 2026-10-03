package com.assoc.service;

import com.assoc.common.PageResult;
import com.assoc.entity.OperationLog;
import com.assoc.entity.SysUser;
import com.assoc.mapper.OperationLogMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.vo.LogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端：操作日志查询（分页、按模块/关键词/时间段筛选）。
 */
@Service
public class AdminLogService {

    private final OperationLogMapper operationLogMapper;
    private final SysUserMapper sysUserMapper;

    public AdminLogService(OperationLogMapper operationLogMapper, SysUserMapper sysUserMapper) {
        this.operationLogMapper = operationLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public PageResult<LogVO> page(String module, String keyword, LocalDateTime startTime,
                                  LocalDateTime endTime, int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(module != null && !module.isBlank(), OperationLog::getModule, module)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(OperationLog::getAction, keyword)
                        .or().like(OperationLog::getDetail, keyword))
                .ge(startTime != null, OperationLog::getCreatedAt, startTime)
                .le(endTime != null, OperationLog::getCreatedAt, endTime)
                .orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, SysUser> users = result.getRecords().stream()
                .map(OperationLog::getUserId).filter(Objects::nonNull).distinct()
                .map(sysUserMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<LogVO> list = result.getRecords().stream().map(logEntry -> {
            SysUser user = logEntry.getUserId() == null ? null : users.get(logEntry.getUserId());
            return new LogVO(logEntry.getId(), logEntry.getUserId(),
                    user == null ? null : user.getRealName(),
                    logEntry.getModule(), logEntry.getAction(), logEntry.getDetail(),
                    logEntry.getIp(), logEntry.getCreatedAt());
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }
}
