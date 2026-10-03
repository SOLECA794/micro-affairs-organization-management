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
 *
 * IP 捕获必须在进入异步线程前同步完成（@Async 线程无 RequestContextHolder，
 * 异步内取恒为 null），因此 record() 先在调用方线程解析 IP，再作为参数传入异步落库。
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /** 业务线程内调用：同步捕获 IP 后交异步线程落库 */
    public void record(Long userId, String module, String action, String detail) {
        String ip = currentIp();
        doRecordAsync(userId, module, action, detail, ip);
    }

    @Async("logExecutor")
    public void doRecordAsync(Long userId, String module, String action, String detail, String ip) {
        try {
            OperationLog entity = new OperationLog();
            entity.setUserId(userId);
            entity.setModule(module);
            entity.setAction(action);
            entity.setDetail(detail);
            entity.setIp(ip);
            entity.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {} - {}", module, action, e);
        }
    }

    public void recordCurrent(String module, String action, String detail) {
        record(UserContext.userId(), module, action, detail);
    }

    /** 必须在请求线程内调用；异步线程中 RequestContextHolder 为空返回 null */
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
