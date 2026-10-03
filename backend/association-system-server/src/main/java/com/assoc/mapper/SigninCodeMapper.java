package com.assoc.mapper;

import com.assoc.entity.SigninCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SigninCodeMapper extends BaseMapper<SigninCode> {

    /** 按 Token 查询签到码 */
    @Select("SELECT * FROM signin_code WHERE token = #{token}")
    SigninCode selectByToken(@Param("token") String token);

    /** 作废活动当前有效签到码（新开作废旧码，02 文档 §5.5） */
    @Update("UPDATE signin_code SET status = 0 WHERE activity_id = #{activityId} AND status = 1")
    int invalidateByActivity(@Param("activityId") Long activityId);
}
