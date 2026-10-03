package com.assoc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 报名表 signup（含候补）。
 */
@TableName("signup")
public class Signup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String status;
    private Integer queueOrder;
    private LocalDateTime signupTime;
    private LocalDateTime cancelTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getQueueOrder() { return queueOrder; }
    public void setQueueOrder(Integer queueOrder) { this.queueOrder = queueOrder; }
    public LocalDateTime getSignupTime() { return signupTime; }
    public void setSignupTime(LocalDateTime signupTime) { this.signupTime = signupTime; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
}
