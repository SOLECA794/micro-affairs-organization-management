package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.ErrorCode;
import com.assoc.entity.Activity;
import com.assoc.entity.Association;
import com.assoc.entity.Attendance;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.AttendanceMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.ActivityListVO;
import com.assoc.vo.ActivityStatisticsVO;
import com.assoc.vo.ManagerOverviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统计（02 文档 §5.6）：
 * 报名人数 = ACTIVE 报名数；签到人数 = attendance 数；缺席 = ACTIVE 且未签到；
 * 到场率 = 签到人数 / 报名成功人数 × 100%。
 */
@Service
public class StatisticsService {

    private final ActivityMapper activityMapper;
    private final AttendanceMapper attendanceMapper;
    private final AssociationMapper associationMapper;
    private final SignupService signupService;
    private final ManagerActivityService managerActivityService;
    private final ActivityService activityService;

    public StatisticsService(ActivityMapper activityMapper, AttendanceMapper attendanceMapper,
                             AssociationMapper associationMapper, SignupService signupService,
                             ManagerActivityService managerActivityService, ActivityService activityService) {
        this.activityMapper = activityMapper;
        this.attendanceMapper = attendanceMapper;
        this.associationMapper = associationMapper;
        this.signupService = signupService;
        this.managerActivityService = managerActivityService;
        this.activityService = activityService;
    }

    /** 单个活动统计 */
    public ActivityStatisticsVO activityStatistics(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        long enrolled = signupService.countActive(activityId);
        long signed = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getActivityId, activityId));
        long absent = Math.max(0, enrolled - signed);
        double rate = enrolled == 0 ? 0.0 : Math.round(signed * 1000.0 / enrolled) / 10.0;
        return new ActivityStatisticsVO(activityId, activity.getTitle(), (int) enrolled, (int) signed,
                (int) absent, rate);
    }

    /** 社团端工作台概览 */
    public ManagerOverviewVO managerOverview() {
        Long managerId = UserContext.userId();
        Association association = managerActivityService.requireOwnAssociation(managerId);
        LambdaQueryWrapper<Activity> baseWrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getAssociationId, association.getId());
        long activityTotal = activityMapper.selectCount(baseWrapper);
        long publishedTotal = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getAssociationId, association.getId())
                .eq(Activity::getStatus, com.assoc.common.Constants.ACT_PUBLISHED));

        List<Activity> activities = activityMapper.selectList(baseWrapper);
        long signupTotal = 0;
        long attendanceTotal = 0;
        for (Activity activity : activities) {
            signupTotal += signupService.countActive(activity.getId());
            attendanceTotal += attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getActivityId, activity.getId()));
        }

        Page<Activity> recent = activityMapper.selectPage(new Page<>(1, 5),
                new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getAssociationId, association.getId())
                        .orderByDesc(Activity::getCreatedAt));
        List<ActivityListVO> recentList = activityService.toListVOs(recent.getRecords());

        return new ManagerOverviewVO(association.getId(), association.getName(),
                activityTotal, publishedTotal, signupTotal, attendanceTotal, recentList);
    }
}
