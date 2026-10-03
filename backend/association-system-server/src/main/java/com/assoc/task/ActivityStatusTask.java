package com.assoc.task;

import com.assoc.mapper.ActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动状态自动流转（02 文档 §5.2）：每分钟把结束时间已过的 PUBLISHED 活动置为 ENDED。
 */
@Component
public class ActivityStatusTask {

    private static final Logger log = LoggerFactory.getLogger(ActivityStatusTask.class);

    private final ActivityMapper activityMapper;

    public ActivityStatusTask(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void closeExpiredActivities() {
        int updated = activityMapper.closeExpiredActivities();
        if (updated > 0) {
            log.info("活动状态自动流转：{} 个已发布活动到达结束时间，置为 ENDED", updated);
        }
    }
}
