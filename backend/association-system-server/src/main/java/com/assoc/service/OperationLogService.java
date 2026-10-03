package com.assoc.service;

import com.assoc.entity.OperationLog;
import com.assoc.mapper.OperationLogMapper;
import com.assoc.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志（02 文档 §5.8）：记录登录、报名、取消、递补、签到、补签、审核、发布/取消/归档等节点。
 * 异步落库，失败不影响业务主流程。
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Async("logExecutor")
    public void record(Long userId, String module, String action, String detail) {
        try {
            OperationLog entity = new OperationLog();
            entity.setUserId(userId);
            entity.setModule(module);
            entity.setAction(action);
            entity.setDetail(detail);
            entity.setIp(currentIp());
            entity.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {} - {}", module, action, e);
        }
    }

    public void recordCurrent(String module, String action, String detail) {
        record(UserContext.userId(), module, action, detail);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
