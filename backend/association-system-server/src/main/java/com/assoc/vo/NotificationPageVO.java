package com.assoc.vo;

import com.assoc.common.PageResult;
import java.util.List;

/** 我的通知分页 + 未读数 */
public record NotificationPageVO(List<NotificationVO> list, long total, long page, long size, long unreadCount) {

    public static NotificationPageVO of(PageResult<NotificationVO> page, long unreadCount) {
        return new NotificationPageVO(page.getList(), page.getTotal(), page.getPage(), page.getSize(), unreadCount);
    }
}
