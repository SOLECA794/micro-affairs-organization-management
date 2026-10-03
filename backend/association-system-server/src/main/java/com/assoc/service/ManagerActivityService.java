package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.dto.ActivityDTO;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 社团端活动管理：创建、修改、提交审核、发布、取消、归档（02 文档 §5.2 状态机）。
 */
@Service
public class ManagerActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final AssociationMapper associationMapper;
    private final SignupMapper signupMapper;
    private final ActivityService activityService;
    private final SignupService signupService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public ManagerActivityService(ActivityMapper activityMapper, ActivityCategoryMapper categoryMapper,
                                  AssociationMapper associationMapper, SignupMapper signupMapper,
                                  ActivityService activityService, SignupService signupService,
                                  NotificationService notificationService,
                                  OperationLogService operationLogService) {
        this.activityMapper = activityMapper;
        this.categoryMapper = categoryMapper;
        this.associationMapper = associationMapper;
        this.signupMapper = signupMapper;
        this.activityService = activityService;
        this.signupService = signupService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    /** 本社团活动列表（含草稿/待审核，按状态、关键词筛选） */
    public PageResult<ActivityListVO> pageOwn(String keyword, String status, int page, int size) {
        Long managerId = UserContext.userId();
        Association association = requireOwnAssociation(managerId);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getAssociationId, association.getId())
                .like(keyword != null && !keyword.isBlank(), Activity::getTitle, keyword)
                .eq(status != null && !status.isBlank(), Activity::getStatus, status)
                .orderByDesc(Activity::getCreatedAt);
        Page<Activity> result = activityMapper.selectPage(new Page<>(page, size), wrapper);
        List<ActivityListVO> list = activityService.toListVOs(result.getRecords());
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 创建活动（草稿） */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ActivityDTO dto) {
        Long managerId = UserContext.userId();
        Association association = requireOwnAssociation(managerId);
        validateTimes(dto);
        Activity activity = new Activity();
        applyDto(activity, dto);
        activity.setAssociationId(association.getId());
        activity.setEnrolledCount(0);
        activity.setStatus(Constants.ACT_DRAFT);
        activity.setCreatedBy(managerId);
        activity.setDeleted(0);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.insert(activity);
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "创建活动",
                "活动: " + activity.getTitle());
        return activity.getId();
    }

    /** 修改草稿/被驳回活动 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long activityId, ActivityDTO dto) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        if (!Constants.ACT_DRAFT.equals(activity.getStatus())
                && !Constants.ACT_REJECTED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "仅草稿或被驳回状态可修改");
        }
        validateTimes(dto);
        applyDto(activity, dto);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "修改活动",
                "活动: " + activity.getTitle());
    }

    /** 提交审核：DRAFT/REJECTED → PENDING */
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        if (!Constants.ACT_DRAFT.equals(activity.getStatus())
                && !Constants.ACT_REJECTED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "仅草稿或被驳回状态可提交审核");
        }
        validateTimes(toDto(activity));
        activity.setStatus(Constants.ACT_PENDING);
        activity.setAuditComment(null);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "提交审核",
                "活动: " + activity.getTitle());
    }

    /** 发布：APPROVED → PUBLISHED；停用社团不可新发布（01 文档 §5.2） */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        if (!Constants.ACT_APPROVED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "仅审核通过的活动可发布");
        }
        Association association = associationMapper.selectById(activity.getAssociationId());
        if (association == null || association.getStatus() == null
                || association.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "社团已停用，活动不可发布");
        }
        activity.setStatus(Constants.ACT_PUBLISHED);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "发布活动",
                "活动: " + activity.getTitle());
    }

    /** 取消活动：PUBLISHED → CANCELLED，通知已报名学生 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        if (!Constants.ACT_PUBLISHED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "仅已发布活动可取消");
        }
        activity.setStatus(Constants.ACT_CANCELLED);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);

        // 通知已报名（含候补）学生
        List<Signup> signups = signupMapper.selectList(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .in(Signup::getStatus, Constants.SIGNUP_ACTIVE, Constants.SIGNUP_WAITING));
        for (Signup signup : signups) {
            notificationService.createQuietly(signup.getUserId(), Constants.NOTIFY_CANCELLED, "活动取消通知",
                    "很抱歉，您报名的活动《" + activity.getTitle() + "》已被取消。");
        }
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "取消活动",
                "活动: " + activity.getTitle() + "，通知人数: " + signups.size());
    }

    /** 归档：ENDED → ARCHIVED，归档后仅可查看与统计 */
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        if (!Constants.ACT_ENDED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "仅已结束活动可归档");
        }
        activity.setStatus(Constants.ACT_ARCHIVED);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        operationLogService.record(managerId, Constants.MODULE_ACTIVITY, "归档活动",
                "活动: " + activity.getTitle());
    }

    /** 本社团活动详情（含简介/封面/分类/审核意见，供编辑页回填） */
    public ActivityDetailVO detailOwn(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = requireOwn(activityId, managerId);
        Association association = associationMapper.selectById(activity.getAssociationId());
        ActivityCategory category = activity.getCategoryId() == null ? null
                : categoryMapper.selectById(activity.getCategoryId());
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
                activity.getAuditComment(), null, null, false, false);
    }

    // ---------- 内部 ----------

    public Association requireOwnAssociation(Long managerId) {
        Association association = associationMapper.selectOne(new LambdaQueryWrapper<Association>()
                .eq(Association::getLeaderUserId, managerId)
                .last("LIMIT 1"));
        if (association == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定社团");
        }
        return association;
    }

    private Activity requireOwn(Long activityId, Long managerId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        signupService.checkOwnership(activity, managerId);
        return activity;
    }

    private void validateTimes(ActivityDTO dto) {
        if (dto.endTime().isBefore(dto.startTime()) || dto.endTime().isEqual(dto.startTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (dto.signupDeadline().isAfter(dto.startTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报名截止时间不能晚于活动开始时间");
        }
        if (dto.categoryId() != null && categoryMapper.selectById(dto.categoryId()) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "活动分类不存在");
        }
    }

    private void applyDto(Activity activity, ActivityDTO dto) {
        activity.setTitle(dto.title());
        activity.setCategoryId(dto.categoryId());
        activity.setCover(dto.cover());
        activity.setDescription(dto.description());
        activity.setLocation(dto.location());
        activity.setStartTime(dto.startTime());
        activity.setEndTime(dto.endTime());
        activity.setSignupDeadline(dto.signupDeadline());
        activity.setCapacity(dto.capacity());
    }

    private ActivityDTO toDto(Activity activity) {
        return new ActivityDTO(activity.getTitle(), activity.getCategoryId(), activity.getCover(),
                activity.getDescription(), activity.getLocation(), activity.getStartTime(),
                activity.getEndTime(), activity.getSignupDeadline(), activity.getCapacity());
    }

    public Map<Long, Association> associationsOf(List<Long> ids) {
        return activityService.loadAssociations(ids);
    }
}
