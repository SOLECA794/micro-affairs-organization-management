package com.assoc.mapper;

import com.assoc.entity.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 报名名额条件更新占位（02 文档 §5.3）：
     * 仅当活动为 PUBLISHED 且未满员时 +1，受影响行数 = 1 才允许插入报名记录。
     */
    @Update("UPDATE activity SET enrolled_count = enrolled_count + 1 "
            + "WHERE id = #{id} AND status = 'PUBLISHED' AND enrolled_count < capacity AND deleted = 0")
    int increaseEnrolled(@Param("id") Long id);

    /**
     * 取消释放名额（02 文档 §5.3）。
     */
    @Update("UPDATE activity SET enrolled_count = enrolled_count - 1 "
            + "WHERE id = #{id} AND enrolled_count > 0")
    int decreaseEnrolled(@Param("id") Long id);

    /**
     * 活动行锁：报名/取消事务内先锁定活动行，串行化同一名额争用。
     */
    @Select("SELECT * FROM activity WHERE id = #{id} FOR UPDATE")
    Activity selectByIdForUpdate(@Param("id") Long id);

    /**
     * 定时任务：把结束时间已过的 PUBLISHED 活动置为 ENDED（02 文档 §5.2）。
     */
    @Update("UPDATE activity SET status = 'ENDED', updated_at = NOW() "
            + "WHERE status = 'PUBLISHED' AND end_time < NOW() AND deleted = 0")
    int closeExpiredActivities();
}
