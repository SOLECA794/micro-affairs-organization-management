package com.assoc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 签到表 attendance。
 */
@TableName("attendance")
public class Attendance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String signType;
    private LocalDateTime signTime;
    private Long operatorId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSignType() { return signType; }
    public void setSignType(String signType) { this.signType = signType; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
}
