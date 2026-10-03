package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.common.PageResult;
import com.assoc.entity.Activity;
import com.assoc.entity.Attendance;
import com.assoc.entity.Association;
import com.assoc.entity.Signup;
import com.assoc.entity.SysUser;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.AttendanceMapper;
import com.assoc.mapper.SignupMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.SignupItemVO;
import com.assoc.vo.SignupResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报名/取消/候补递补（02 文档 §5.3、§5.4）。
 *
 * 并发控制三层保障：
 * 1. UNIQUE(activity_id, user_id) 唯一约束防重复报名；
 * 2. 条件 UPDATE activity SET enrolled_count=enrolled_count+1 WHERE status='PUBLISHED' AND enrolled_count<capacity
 *    占位名额，与报名记录写入同一事务，受影响行数=1 才继续；
 * 3. 事务内再次校验活动状态为 PUBLISHED、当前服务器时间未过 signup_deadline；
 * 另：事务内以 SELECT ... FOR UPDATE 锁定活动行，串行化同一活动的名额争用，候补序号不重复。
 */
@Service
public class SignupService {

    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    private final ActivityMapper activityMapper;
    private final SignupMapper signupMapper;
    private final AssociationMapper associationMapper;
    private final AttendanceMapper attendanceMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public SignupService(ActivityMapper activityMapper, SignupMapper signupMapper,
                         AssociationMapper associationMapper, AttendanceMapper attendanceMapper,
                         SysUserMapper sysUserMapper, NotificationService notificationService,
                         OperationLogService operationLogService) {
        this.activityMapper = activityMapper;
        this.signupMapper = signupMapper;
        this.associationMapper = associationMapper;
        this.attendanceMapper = attendanceMapper;
        this.sysUserMapper = sysUserMapper;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    /** 报名（成功或进入候补） */
    @Transactional(rollbackFor = Exception.class)
    public SignupResultVO signup(Long activityId) {
        Long userId = UserContext.userId();
        LocalDateTime now = LocalDateTime.now();

        // 事务内状态与时间校验（锁定活动行，串行化名额争用）
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (!Constants.ACT_PUBLISHED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "活动当前状态不可报名");
        }
        if (activity.getSignupDeadline() != null && now.isAfter(activity.getSignupDeadline())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "报名已截止");
        }

        Signup existing = signupMapper.selectByActivityAndUserForUpdate(activityId, userId);
        if (existing != null && (Constants.SIGNUP_ACTIVE.equals(existing.getStatus())
                || Constants.SIGNUP_WAITING.equals(existing.getStatus()))) {
            throw new BusinessException(ErrorCode.DUPLICATE, "请勿重复报名");
        }

        // 条件更新占位名额：受影响行数 = 1 才报名成功，否则进入候补
        int affected = activityMapper.increaseEnrolled(activityId);
        Signup signup = existing != null ? existing : new Signup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setSignupTime(now);
        signup.setCancelTime(null);
        signup.setQueueOrder(0);

        String result;
        if (affected == 1) {
            result = Constants.SIGNUP_ACTIVE;
            signup.setStatus(Constants.SIGNUP_ACTIVE);
            saveSignup(signup, existing != null);
            notificationService.create(userId, Constants.NOTIFY_SIGNUP, "报名成功",
                    "您已成功报名活动《" + activity.getTitle() + "》，请在活动开始前到场签到。");
            operationLogService.record(userId, Constants.MODULE_SIGNUP, "报名成功",
                    "活动: " + activity.getTitle());
        } else {
            result = Constants.SIGNUP_WAITING;
            int queueOrder = signupMapper.selectMaxQueueOrder(activityId) + 1;
            signup.setStatus(Constants.SIGNUP_WAITING);
            signup.setQueueOrder(queueOrder);
            saveSignup(signup, existing != null);
            notificationService.create(userId, Constants.NOTIFY_WAITING, "已进入候补",
                    "活动《" + activity.getTitle() + "》名额已满，您已进入候补，当前候补序号第 " + queueOrder + " 位。");
            operationLogService.record(userId, Constants.MODULE_SIGNUP, "进入候补",
                    "活动: " + activity.getTitle() + "，序号: " + queueOrder);
        }
        return new SignupResultVO(signup.getId(), result,
                Constants.SIGNUP_WAITING.equals(result) ? signup.getQueueOrder() : null);
    }

    /** 取消报名（截止前）：释放名额并按候补顺序递补 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long activityId) {
        Long userId = UserContext.userId();
        LocalDateTime now = LocalDateTime.now();

        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getSignupDeadline() != null && now.isAfter(activity.getSignupDeadline())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "已过报名截止时间，不可取消报名");
        }
        Signup signup = signupMapper.selectByActivityAndUserForUpdate(activityId, userId);
        if (signup == null || Constants.SIGNUP_CANCELLED.equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "当前无有效报名记录");
        }

        boolean freedSlot = Constants.SIGNUP_ACTIVE.equals(signup.getStatus());
        if (freedSlot) {
            // 释放名额（02 文档 §5.3）
            activityMapper.decreaseEnrolled(activityId);
        }
        signupMapper.update(null, new LambdaUpdateWrapper<Signup>()
                .eq(Signup::getId, signup.getId())
                .set(Signup::getStatus, Constants.SIGNUP_CANCELLED)
                .set(Signup::getQueueOrder, 0)
                .set(Signup::getCancelTime, now));
        operationLogService.record(userId, Constants.MODULE_SIGNUP, "取消报名",
                "活动: " + activity.getTitle());

        // 递补仅在取消事务内触发，避免并发重复递补（02 文档 §5.4）
        if (freedSlot) {
            Signup next = signupMapper.selectFirstWaiting(activityId);
            if (next != null) {
                signupMapper.update(null, new LambdaUpdateWrapper<Signup>()
                        .eq(Signup::getId, next.getId())
                        .set(Signup::getStatus, Constants.SIGNUP_ACTIVE)
                        .set(Signup::getQueueOrder, 0));
                notificationService.create(next.getUserId(), Constants.NOTIFY_PROMOTED, "候补递补成功",
                        "活动《" + activity.getTitle() + "》有名额释放，您已由候补递补为报名成功。");
                operationLogService.record(next.getUserId(), Constants.MODULE_SIGNUP, "候补递补",
                        "活动: " + activity.getTitle() + "，递补用户: " + next.getUserId());
                log.info("递补完成 activityId={} signupId={} userId={}", activityId, next.getId(), next.getUserId());
            }
        }
    }

    /** 社团端报名名单（按状态筛选、分页，含是否已签到标记） */
    public PageResult<SignupItemVO> signupList(Long activityId, String status, int page, int size) {
        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(status != null && !status.isBlank(), Signup::getStatus, status)
                .orderByAsc(Signup::getStatus)
                .orderByAsc(Signup::getQueueOrder)
                .orderByAsc(Signup::getSignupTime);
        Page<Signup> result = signupMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, SysUser> users = result.getRecords().stream()
                .map(Signup::getUserId).distinct()
                .collect(Collectors.toMap(Function.identity(),
                        id -> {
                            SysUser u = sysUserMapper.selectById(id);
                            return u != null ? u : new SysUser();
                        }));
        Map<Long, Attendance> attendances = attendanceMapper.selectList(
                        new LambdaQueryWrapper<Attendance>()
                                .eq(Attendance::getActivityId, activityId))
                .stream()
                .collect(Collectors.toMap(Attendance::getUserId, Function.identity()));

        List<SignupItemVO> list = result.getRecords().stream().map(signup -> {
            SysUser user = users.getOrDefault(signup.getUserId(), new SysUser());
            return new SignupItemVO(
                    signup.getId(), signup.getUserId(), user.getRealName(), user.getPhone(),
                    signup.getStatus(), signup.getQueueOrder(), signup.getSignupTime(),
                    attendances.containsKey(signup.getUserId()));
        }).toList();
        return new PageResult<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 统计口径：报名成功人数（02 文档 §5.6） */
    public long countActive(Long activityId) {
        return signupMapper.selectCount(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getStatus, Constants.SIGNUP_ACTIVE));
    }

    /** 已报名（ACTIVE）用户列表：用于取消活动通知与开启签到提醒 */
    public List<Long> activeUserIds(Long activityId) {
        return signupMapper.selectList(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getStatus, Constants.SIGNUP_ACTIVE))
                .stream().map(Signup::getUserId).toList();
    }

    /** 校验活动归属：负责人只能操作本社团活动，越权返回 403 */
    public void checkOwnership(Activity activity, Long managerUserId) {
        Association association = associationMapper.selectOne(new LambdaQueryWrapper<Association>()
                .eq(Association::getLeaderUserId, managerUserId)
                .last("LIMIT 1"));
        if (association == null || !association.getId().equals(activity.getAssociationId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作其他社团的活动");
        }
    }

    private void saveSignup(Signup signup, boolean isUpdate) {
        if (isUpdate) {
            signupMapper.update(null, new LambdaUpdateWrapper<Signup>()
                    .eq(Signup::getId, signup.getId())
                    .set(Signup::getStatus, signup.getStatus())
                    .set(Signup::getQueueOrder, signup.getQueueOrder())
                    .set(Signup::getSignupTime, signup.getSignupTime())
                    .set(Signup::getCancelTime, null));
        } else {
            try {
                signupMapper.insert(signup);
            } catch (DuplicateKeyException e) {
                // UNIQUE(activity_id, user_id) 并发兜底
                throw new BusinessException(ErrorCode.DUPLICATE, "请勿重复报名");
            }
        }
    }
}
