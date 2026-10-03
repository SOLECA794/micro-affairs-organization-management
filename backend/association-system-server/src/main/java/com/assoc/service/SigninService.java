package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.entity.Activity;
import com.assoc.entity.Attendance;
import com.assoc.entity.SigninCode;
import com.assoc.entity.Signup;
import com.assoc.entity.SysUser;
import com.assoc.config.AppProperties;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AttendanceMapper;
import com.assoc.mapper.SigninCodeMapper;
import com.assoc.mapper.SignupMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.OpenSigninVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 签到（02 文档 §5.5）：限时二维码、扫码四重校验、人工补签、异常日志。
 */
@Service
public class SigninService {

    private static final Logger log = LoggerFactory.getLogger(SigninService.class);

    /** 签到码有效期（分钟） */
    private static final int CODE_VALID_MINUTES = 5;

    private final ActivityMapper activityMapper;
    private final SigninCodeMapper signinCodeMapper;
    private final AttendanceMapper attendanceMapper;
    private final SignupMapper signupMapper;
    private final SysUserMapper sysUserMapper;
    private final SignupService signupService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    private final AppProperties properties;

    public SigninService(ActivityMapper activityMapper, SigninCodeMapper signinCodeMapper,
                         AttendanceMapper attendanceMapper, SignupMapper signupMapper,
                         SysUserMapper sysUserMapper, SignupService signupService,
                         NotificationService notificationService, OperationLogService operationLogService,
                         AppProperties properties) {
        this.activityMapper = activityMapper;
        this.signinCodeMapper = signinCodeMapper;
        this.attendanceMapper = attendanceMapper;
        this.signupMapper = signupMapper;
        this.sysUserMapper = sysUserMapper;
        this.signupService = signupService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
        this.properties = properties;
    }

    /** 开启签到：生成限时签到码，同活动同时刻仅一个有效码（新开作废旧码） */
    @Transactional(rollbackFor = Exception.class)
    public OpenSigninVO open(Long activityId) {
        Long managerId = UserContext.userId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        signupService.checkOwnership(activity, managerId);
        if (!Constants.ACT_PUBLISHED.equals(activity.getStatus())
                && !Constants.ACT_ENDED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "活动当前状态不可开启签到");
        }

        // 作废旧码
        signinCodeMapper.invalidateByActivity(activityId);

        SigninCode code = new SigninCode();
        code.setActivityId(activityId);
        code.setToken(UUID.randomUUID().toString().replace("-", ""));
        LocalDateTime now = LocalDateTime.now();
        code.setExpiresAt(now.plusMinutes(CODE_VALID_MINUTES));
        code.setStatus(1);
        code.setCreatedBy(managerId);
        code.setCreatedAt(now);
        signinCodeMapper.insert(code);

        // 开启签到 → 通知已报名学生（签到入口提醒）
        List<Long> userIds = signupService.activeUserIds(activityId);
        for (Long userId : userIds) {
            notificationService.createQuietly(userId, Constants.NOTIFY_SIGNIN, "签到提醒",
                    "活动《" + activity.getTitle() + "》已开启签到，请在有效期内扫码完成签到。");
        }
        operationLogService.record(managerId, Constants.MODULE_SIGNIN, "开启签到",
                "活动: " + activity.getTitle() + "，有效期至: " + code.getExpiresAt());

        String qrcodeUrl = properties.getQrcode().getBaseUrl() + "/api/signin/qrcode?token=" + code.getToken();
        return new OpenSigninVO(code.getToken(), code.getExpiresAt(), qrcodeUrl);
    }

    /** 扫码签到：四重校验（签到码 / 活动状态 / 报名状态 / 未重复签到） */
    @Transactional(rollbackFor = Exception.class)
    public Attendance scan(Long activityId, String token) {
        Long userId = UserContext.userId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 签到码存在、状态有效、未过期
        SigninCode code = signinCodeMapper.selectByToken(token);
        if (code == null || code.getStatus() == null || code.getStatus() != 1) {
            recordAbnormal(userId, activityId, token, "无效签到码");
            throw new BusinessException(ErrorCode.PARAM_ERROR, "签到码无效或已过期");
        }
        if (code.getExpiresAt() == null || now.isAfter(code.getExpiresAt())) {
            recordAbnormal(userId, activityId, token, "扫码时签到码已过期");
            throw new BusinessException(ErrorCode.PARAM_ERROR, "签到码无效或已过期");
        }
        if (!code.getActivityId().equals(activityId)) {
            recordAbnormal(userId, activityId, token, "签到码与活动不匹配");
            throw new BusinessException(ErrorCode.PARAM_ERROR, "签到码无效或已过期");
        }

        // 2. 活动状态允许签到
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (!Constants.ACT_PUBLISHED.equals(activity.getStatus())
                && !Constants.ACT_ENDED.equals(activity.getStatus())) {
            recordAbnormal(userId, activityId, token, "活动状态不可签到: " + activity.getStatus());
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "活动当前状态不可签到");
        }

        // 3. 报名状态为 ACTIVE（未报名不可签到）
        Signup signup = signupMapper.selectByActivityAndUser(activityId, userId);
        if (signup == null || !Constants.SIGNUP_ACTIVE.equals(signup.getStatus())) {
            recordAbnormal(userId, activityId, token, "未报名或报名已取消/候补中");
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "未报名成功，无法签到");
        }

        // 4. 未重复签到（UNIQUE(activity_id, user_id) 兜底）
        Attendance attendance = new Attendance();
        attendance.setActivityId(activityId);
        attendance.setUserId(userId);
        attendance.setSignType(Constants.SIGN_TYPE_QR);
        attendance.setSignTime(now);
        attendance.setOperatorId(null);
        try {
            attendanceMapper.insert(attendance);
        } catch (DuplicateKeyException e) {
            recordAbnormal(userId, activityId, token, "重复扫码签到");
            throw new BusinessException(ErrorCode.DUPLICATE, "请勿重复签到");
        }
        operationLogService.record(userId, Constants.MODULE_SIGNIN, "扫码签到",
                "活动: " + activity.getTitle());
        return attendance;
    }

    /** 人工补签：名单内 ACTIVE 且未签到成员，记录操作人与时间 */
    @Transactional(rollbackFor = Exception.class)
    public Attendance manual(Long activityId, Long targetUserId) {
        Long managerId = UserContext.userId();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        signupService.checkOwnership(activity, managerId);

        Signup signup = signupMapper.selectByActivityAndUser(activityId, targetUserId);
        if (signup == null || !Constants.SIGNUP_ACTIVE.equals(signup.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "该成员不在报名成功名单中，无法补签");
        }
        Long signed = attendanceMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getActivityId, activityId)
                .eq(Attendance::getUserId, targetUserId));
        if (signed > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE, "该成员已签到，请勿重复补签");
        }

        Attendance attendance = new Attendance();
        attendance.setActivityId(activityId);
        attendance.setUserId(targetUserId);
        attendance.setSignType(Constants.SIGN_TYPE_MANUAL);
        attendance.setSignTime(LocalDateTime.now());
        attendance.setOperatorId(managerId);
        attendanceMapper.insert(attendance);
        operationLogService.record(managerId, Constants.MODULE_SIGNIN, "人工补签",
                "活动: " + activity.getTitle() + "，补签用户: " + targetUserId);
        return attendance;
    }

    /** 签到名单（社团端；归属校验防横向越权） */
    public List<com.assoc.vo.AttendanceItemVO> attendanceList(Long activityId) {
        signupService.requireOwnership(activityId);
        List<Attendance> records = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getActivityId, activityId)
                .orderByAsc(Attendance::getSignTime));
        Map<Long, SysUser> users = records.stream().map(Attendance::getUserId).distinct()
                .map(sysUserMapper::selectById).filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysUser> operators = records.stream().map(Attendance::getOperatorId)
                .filter(java.util.Objects::nonNull).distinct()
                .map(sysUserMapper::selectById).filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return records.stream().map(record -> {
            SysUser user = users.get(record.getUserId());
            SysUser operator = record.getOperatorId() == null ? null : operators.get(record.getOperatorId());
            return new com.assoc.vo.AttendanceItemVO(
                    record.getId(), record.getUserId(),
                    user == null ? null : user.getRealName(),
                    user == null ? null : user.getPhone(),
                    record.getSignType(), record.getSignTime(),
                    operator == null ? null : operator.getRealName());
        }).toList();
    }

    /** 未签到的报名成功成员（补签候选；归属校验防横向越权） */
    public List<com.assoc.vo.SignupItemVO> unsignedActiveSignups(Long activityId) {
        signupService.requireOwnership(activityId);
        LambdaQueryWrapper<Signup> wrapper = new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .eq(Signup::getStatus, Constants.SIGNUP_ACTIVE);
        List<Signup> signups = signupMapper.selectList(wrapper);
        Map<Long, Attendance> attendances = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getActivityId, activityId))
                .stream().collect(Collectors.toMap(Attendance::getUserId, Function.identity()));
        return signups.stream()
                .filter(signup -> !attendances.containsKey(signup.getUserId()))
                .map(signup -> {
                    SysUser user = sysUserMapper.selectById(signup.getUserId());
                    return new com.assoc.vo.SignupItemVO(
                            signup.getId(), signup.getUserId(),
                            user == null ? null : user.getRealName(),
                            user == null ? null : user.getPhone(),
                            signup.getStatus(), signup.getQueueOrder(), signup.getSignupTime(), false);
                }).toList();
    }

    private void recordAbnormal(Long userId, Long activityId, String token, String reason) {
        log.warn("签到异常 userId={} activityId={} token={} reason={}", userId, activityId, token, reason);
        operationLogService.record(userId, Constants.MODULE_SIGNIN, "签到异常",
                "activityId: " + activityId + "，原因: " + reason);
    }
}
