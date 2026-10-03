package com.assoc.service;

import com.assoc.common.PageResult;
import com.assoc.entity.Notification;
import com.assoc.mapper.NotificationMapper;
import com.assoc.vo.NotificationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知（02 文档 §5.7）：审核结果、报名成功、进入候补、递补成功、活动取消、开启签到。
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /** 写入一条通知（在业务事务内调用，保证与业务一致） */
    public void create(Long userId, String type, String title, String content) {
        Notification entity = new Notification();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setReadStatus(0);
        entity.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(entity);
    }

    /** 批量通知失败不阻断业务（如开启签到群发提醒） */
    public void createQuietly(Long userId, String type, String title, String content) {
        try {
            create(userId, type, title, content);
        } catch (Exception e) {
            log.warn("通知写入失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 我的通知分页 + 未读数 */
    public PageResult<NotificationVO> page(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        List<NotificationVO> list = result.getRecords().stream()
                .map(n -> new NotificationVO(n.getId(), n.getType(), n.getTitle(), n.getContent(),
                        n.getReadStatus(), n.getCreatedAt()))
                .toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadStatus, 0));
    }

    /** 标记本人通知已读；非本人通知返回 40400 */
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new com.assoc.common.BusinessException(com.assoc.common.ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (notification.getReadStatus() == null || notification.getReadStatus() == 0) {
            notification.setReadStatus(1);
            notificationMapper.updateById(notification);
        }
    }
}
