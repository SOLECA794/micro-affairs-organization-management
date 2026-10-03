package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.entity.Activity;
import com.assoc.entity.ActivityCategory;
import com.assoc.entity.Association;
import com.assoc.entity.Signup;
import com.assoc.mapper.ActivityCategoryMapper;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.SignupMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.ActivityDetailVO;
import com.assoc.vo.ActivityListVO;
import com.assoc.vo.MySignupVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生端活动查询：列表（按社团、时间、状态、关键词，分页）、详情（剩余名额、报名状态）、我的报名。
 */
@Service
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final AssociationMapper associationMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final SignupMapper signupMapper;

    public ActivityService(ActivityMapper activityMapper, AssociationMapper associationMapper,
                           ActivityCategoryMapper categoryMapper, SignupMapper signupMapper) {
        this.activityMapper = activityMapper;
        this.associationMapper = associationMapper;
        this.categoryMapper = categoryMapper;
        this.signupMapper = signupMapper;
    }

    /**
     * 公开活动列表。学生端默认展示“报名中/即将开始”，即未传 status 时默认 PUBLISHED
     * （时间判断以服务器时间为准，前端不参与业务判定）。
     */
    public PageResult<ActivityListVO> pageActivities(String keyword, Long associationId, Long categoryId,
                                                     String status, LocalDateTime startTimeBegin,
                                                     LocalDateTime startTimeEnd, int page, int size) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .like(keyword != null && !keyword.isBlank(), Activity::getTitle, keyword)
                .eq(associationId != null, Activity::getAssociationId, associationId)
                .eq(categoryId != null, Activity::getCategoryId, categoryId)
                .eq(status != null && !status.isBlank(), Activity::getStatus,
                        status == null || status.isBlank() ? Constants.ACT_PUBLISHED : status)
                .ge(startTimeBegin != null, Activity::getStartTime, startTimeBegin)
                .le(startTimeEnd != null, Activity::getStartTime, startTimeEnd)
                .orderByDesc(Activity::getStartTime);
        Page<Activity> result = activityMapper.selectPage(new Page<>(page, size), wrapper);
        List<ActivityListVO> list = toListVOs(result.getRecords());
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 学生端活动详情：含剩余名额、本人报名状态、可报名/可取消标记 */
    public ActivityDetailVO detail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        // 草稿/待审核/被驳回活动对学生不可见
        if (Constants.ACT_DRAFT.equals(activity.getStatus()) || Constants.ACT_PENDING.equals(activity.getStatus())
                || Constants.ACT_REJECTED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在或未发布");
        }

        Association association = associationMapper.selectById(activity.getAssociationId());
        ActivityCategory category = activity.getCategoryId() == null ? null
                : categoryMapper.selectById(activity.getCategoryId());
        Long userId = UserContext.userId();
        Signup mySignup = userId == null ? null
                : signupMapper.selectByActivityAndUser(activityId, userId);
        LocalDateTime now = LocalDateTime.now();
        boolean deadlinePassed = activity.getSignupDeadline() != null && now.isAfter(activity.getSignupDeadline());
        boolean hasActiveSignup = mySignup != null
                && (Constants.SIGNUP_ACTIVE.equals(mySignup.getStatus())
                || Constants.SIGNUP_WAITING.equals(mySignup.getStatus()));
        boolean assocNormal = association != null && association.getStatus() != null
                && association.getStatus() == Constants.STATUS_ENABLED;
        boolean canSignup = Constants.ACT_PUBLISHED.equals(activity.getStatus())
                && !deadlinePassed && !hasActiveSignup && assocNormal;
        boolean canCancel = hasActiveSignup && !deadlinePassed;

        int remaining = Math.max(0,
                (activity.getCapacity() == null ? 0 : activity.getCapacity())
                        - (activity.getEnrolledCount() == null ? 0 : activity.getEnrolledCount()));

        return new ActivityDetailVO(
                activity.getId(), activity.getTitle(), activity.getAssociationId(), activity.getCategoryId(),
                association == null ? null : association.getName(),
                category == null ? null : category.getName(),
                activity.getCover(), activity.getDescription(), activity.getLocation(),
                activity.getStartTime(), activity.getEndTime(), activity.getSignupDeadline(),
                activity.getCapacity(), activity.getEnrolledCount(), remaining, activity.getStatus(),
                activity.getAuditComment(),
                mySignup == null ? null : mySignup.getStatus(),
                mySignup == null ? null : mySignup.getQueueOrder(),
                canSignup, canCancel);
    }

    /** 我的报名（状态：已报名/候补/已取消，可按状态筛选） */
    public PageResult<MySignupVO> mySignups(String status, int page, int size) {
        Long userId = UserContext.userId();
        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<Signup>()
                .eq(Signup::getUserId, userId)
                .eq(status != null && !status.isBlank(), Signup::getStatus, status)
                .orderByDesc(Signup::getSignupTime);
        Page<Signup> result = signupMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, Activity> activities = loadActivities(
                result.getRecords().stream().map(Signup::getActivityId).toList());
        Map<Long, Association> associations = loadAssociations(
                activities.values().stream().map(Activity::getAssociationId).toList());

        List<MySignupVO> list = result.getRecords().stream().map(signup -> {
            Activity activity = activities.get(signup.getActivityId());
            Association association = activity == null ? null : associations.get(activity.getAssociationId());
            return new MySignupVO(
                    signup.getId(), signup.getActivityId(),
                    activity == null ? null : activity.getTitle(),
                    association == null ? null : association.getName(),
                    activity == null ? null : activity.getLocation(),
                    activity == null ? null : activity.getStartTime(),
                    activity == null ? null : activity.getStatus(),
                    signup.getStatus(), signup.getQueueOrder(),
                    signup.getSignupTime(), signup.getCancelTime());
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ---------- 共用装配 ----------

    public List<ActivityListVO> toListVOs(List<Activity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Association> associations = loadAssociations(
                records.stream().map(Activity::getAssociationId).toList());
        Map<Long, ActivityCategory> categories = loadCategories(
                records.stream().map(Activity::getCategoryId).filter(Objects::nonNull).toList());
        return records.stream().map(activity -> new ActivityListVO(
                activity.getId(), activity.getTitle(),
                name(associations, activity.getAssociationId(), Association::getName),
                activity.getCategoryId() == null ? null
                        : name(categories, activity.getCategoryId(), ActivityCategory::getName),
                activity.getLocation(), activity.getStartTime(), activity.getEndTime(),
                activity.getSignupDeadline(), activity.getCapacity(), activity.getEnrolledCount(),
                activity.getStatus())).toList();
    }

    private Map<Long, Activity> loadActivities(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .map(activityMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
    }

    public Map<Long, Association> loadAssociations(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .map(associationMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(Association::getId, Function.identity()));
    }

    public Map<Long, ActivityCategory> loadCategories(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct()
                .map(categoryMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(ActivityCategory::getId, Function.identity()));
    }

    private static <T> String name(Map<Long, T> map, Long id, Function<T, String> extractor) {
        T value = map.get(id);
        return value == null ? null : extractor.apply(value);
    }
}
