package com.assoc.mapper;

import com.assoc.entity.Signup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SignupMapper extends BaseMapper<Signup> {

    /** 查询本人某活动的报名记录（含行锁，用于报名事务防并发重复） */
    @Select("SELECT * FROM signup WHERE activity_id = #{activityId} AND user_id = #{userId} FOR UPDATE")
    Signup selectByActivityAndUserForUpdate(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /** 查询本人某活动的报名记录（无锁） */
    @Select("SELECT * FROM signup WHERE activity_id = #{activityId} AND user_id = #{userId}")
    Signup selectByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /** 当前候补最大序号 */
    @Select("SELECT COALESCE(MAX(queue_order), 0) FROM signup "
            + "WHERE activity_id = #{activityId} AND status = 'WAITING'")
    int selectMaxQueueOrder(@Param("activityId") Long activityId);

    /** 候补队列第一条（按 queue_order ASC，02 文档 §5.4） */
    @Select("SELECT * FROM signup WHERE activity_id = #{activityId} AND status = 'WAITING' "
            + "ORDER BY queue_order ASC LIMIT 1")
    Signup selectFirstWaiting(@Param("activityId") Long activityId);
}
