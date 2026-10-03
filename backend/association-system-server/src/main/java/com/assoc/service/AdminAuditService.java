package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.entity.Activity;
import com.assoc.entity.ActivityCategory;
import com.assoc.entity.Association;
import com.assoc.mapper.ActivityCategoryMapper;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.AuditItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端：活动审核（通过/驳回，驳回必填意见；结果站内通知负责人）。
 */
@Service
public class AdminAuditService {

    private final ActivityMapper activityMapper;
    private final AssociationMapper associationMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public AdminAuditService(ActivityMapper activityMapper, AssociationMapper associationMapper,
                             ActivityCategoryMapper categoryMapper, NotificationService notificationService,
                             OperationLogService operationLogService) {
        this.activityMapper = activityMapper;
        this.associationMapper = associationMapper;
        this.categoryMapper = categoryMapper;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    /** 待审核（或按状态筛选）活动列表 */
    public PageResult<AuditItemVO> page(String status, String keyword, int page, int size) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(status == null || status.isBlank(), Activity::getStatus, Constants.ACT_PENDING)
                .eq(status != null && !status.isBlank(), Activity::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Activity::getTitle, keyword)
                .orderByDesc(Activity::getUpdatedAt);
        Page<Activity> result = activityMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, Association> associations = result.getRecords().stream()
                .map(Activity::getAssociationId).distinct()
                .map(associationMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(Association::getId, Function.identity()));
        Map<Long, ActivityCategory> categories = result.getRecords().stream()
                .map(Activity::getCategoryId).filter(Objects::nonNull).distinct()
                .map(categoryMapper::selectById).filter(Objects::nonNull)
                .collect(Collectors.toMap(ActivityCategory::getId, Function.identity()));

        List<AuditItemVO> list = result.getRecords().stream().map(activity -> {
            Association association = associations.get(activity.getAssociationId());
            ActivityCategory category = activity.getCategoryId() == null ? null
                    : categories.get(activity.getCategoryId());
            return new AuditItemVO(activity.getId(), activity.getTitle(), activity.getAssociationId(),
                    association == null ? null : association.getName(),
                    category == null ? null : category.getName(),
                    activity.getLocation(), activity.getStartTime(), activity.getEndTime(),
                    activity.getSignupDeadline(), activity.getCapacity(), activity.getStatus(),
                    activity.getAuditComment(), activity.getDescription(), activity.getCreatedAt());
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 审核通过：PENDING → APPROVED，通知负责人 */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long activityId) {
        Activity activity = requirePending(activityId);
        activity.setStatus(Constants.ACT_APPROVED);
        activity.setAuditComment(null);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        notifyLeader(activity, "审核通过", "您提交的活动《" + activity.getTitle() + "》已审核通过，可进行发布。");
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "审核通过",
                "活动: " + activity.getTitle());
    }

    /** 驳回：PENDING → REJECTED（意见必填），通知负责人 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long activityId, String comment) {
        Activity activity = requirePending(activityId);
        activity.setStatus(Constants.ACT_REJECTED);
        activity.setAuditComment(comment);
        activity.setUpdatedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        notifyLeader(activity, "审核驳回", "您提交的活动《" + activity.getTitle() + "》被驳回，意见: " + comment);
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "审核驳回",
                "活动: " + activity.getTitle() + "，意见: " + comment);
    }

    private Activity requirePending(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (!Constants.ACT_PENDING.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "活动不在待审核状态");
        }
        return activity;
    }

    private void notifyLeader(Activity activity, String title, String content) {
        Association association = associationMapper.selectById(activity.getAssociationId());
        if (association != null && association.getLeaderUserId() != null) {
            notificationService.createQuietly(association.getLeaderUserId(), Constants.NOTIFY_AUDIT,
                    title, content);
        }
    }
}
