package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.entity.Activity;
import com.assoc.entity.Association;
import com.assoc.entity.Attendance;
import com.assoc.entity.Signup;
import com.assoc.entity.SysUser;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.AttendanceMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.mapper.SignupMapper;
import com.assoc.mapper.SysUserMapper;
import com.assoc.security.UserContext;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 名单导出（02 文档 §5.6）：EasyExcel 生成报名名单/签到名单，字段与页面列表一致，文件名含活动名与导出时间。
 */
@Service
public class ExportService {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final SignupMapper signupMapper;
    private final AttendanceMapper attendanceMapper;
    private final SysUserMapper sysUserMapper;
    private final AssociationMapper associationMapper;
    private final SignupService signupService;

    public ExportService(ActivityMapper activityMapper, SignupMapper signupMapper,
                         AttendanceMapper attendanceMapper, SysUserMapper sysUserMapper,
                         AssociationMapper associationMapper, SignupService signupService) {
        this.activityMapper = activityMapper;
        this.signupMapper = signupMapper;
        this.attendanceMapper = attendanceMapper;
        this.sysUserMapper = sysUserMapper;
        this.associationMapper = associationMapper;
        this.signupService = signupService;
    }

    /** 报名名单行：与报名名单页面字段一致 */
    public record SignupExportRow(
            @ExcelProperty("序号") Integer index,
            @ExcelProperty("姓名") String realName,
            @ExcelProperty("手机号") String phone,
            @ExcelProperty("报名状态") String status,
            @ExcelProperty("候补序号") Integer queueOrder,
            @ExcelProperty("报名时间") String signupTime) {
    }

    /** 签到名单行：与签到名单页面字段一致 */
    public record AttendanceExportRow(
            @ExcelProperty("序号") Integer index,
            @ExcelProperty("姓名") String realName,
            @ExcelProperty("手机号") String phone,
            @ExcelProperty("签到方式") String signType,
            @ExcelProperty("签到时间") String signTime,
            @ExcelProperty("补签操作人") String operatorName) {
    }

    public void exportSignups(Long activityId, HttpServletResponse response) {
        Activity activity = checkOwn(activityId);
        List<Signup> signups = signupMapper.selectList(new LambdaQueryWrapper<Signup>()
                .eq(Signup::getActivityId, activityId)
                .orderByAsc(Signup::getStatus)
                .orderByAsc(Signup::getQueueOrder)
                .orderByAsc(Signup::getSignupTime));
        Map<Long, SysUser> users = loadUsers(signups.stream().map(Signup::getUserId).toList());

        List<SignupExportRow> rows = new ArrayList<>();
        int index = 1;
        for (Signup signup : signups) {
            SysUser user = users.get(signup.getUserId());
            rows.add(new SignupExportRow(index++,
                    user == null ? null : user.getRealName(),
                    user == null ? null : user.getPhone(),
                    signupStatusText(signup.getStatus()),
                    signup.getQueueOrder() == null || signup.getQueueOrder() == 0 ? null : signup.getQueueOrder(),
                    signup.getSignupTime() == null ? null : DISPLAY_TIME.format(signup.getSignupTime())));
        }
        write(response, "报名名单", activityTitle(activity), SignupExportRow.class, rows);
    }

    public void exportAttendance(Long activityId, HttpServletResponse response) {
        Activity activity = checkOwn(activityId);
        List<Attendance> records = attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getActivityId, activityId)
                .orderByAsc(Attendance::getSignTime));
        Map<Long, SysUser> users = loadUsers(records.stream().map(Attendance::getUserId).toList());
        Map<Long, SysUser> operators = loadUsers(records.stream().map(Attendance::getOperatorId)
                .filter(java.util.Objects::nonNull).toList());

        List<AttendanceExportRow> rows = new ArrayList<>();
        int index = 1;
        for (Attendance record : records) {
            SysUser user = users.get(record.getUserId());
            SysUser operator = record.getOperatorId() == null ? null : operators.get(record.getOperatorId());
            rows.add(new AttendanceExportRow(index++,
                    user == null ? null : user.getRealName(),
                    user == null ? null : user.getPhone(),
                    Constants.SIGN_TYPE_MANUAL.equals(record.getSignType()) ? "补签" : "扫码",
                    record.getSignTime() == null ? null : DISPLAY_TIME.format(record.getSignTime()),
                    operator == null ? null : operator.getRealName()));
        }
        write(response, "签到名单", activityTitle(activity), AttendanceExportRow.class, rows);
    }

    // ---------- 内部 ----------

    private Activity checkOwn(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        signupService.checkOwnership(activity, UserContext.userId());
        return activity;
    }

    private String activityTitle(Activity activity) {
        return activity.getTitle() == null ? "activity-" + activity.getId() : activity.getTitle();
    }

    private String signupStatusText(String status) {
        if (Constants.SIGNUP_ACTIVE.equals(status)) {
            return "已报名";
        }
        if (Constants.SIGNUP_WAITING.equals(status)) {
            return "候补中";
        }
        if (Constants.SIGNUP_CANCELLED.equals(status)) {
            return "已取消";
        }
        return status;
    }

    private Map<Long, SysUser> loadUsers(List<Long> ids) {
        return ids.stream().filter(java.util.Objects::nonNull).distinct()
                .map(sysUserMapper::selectById).filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private void write(HttpServletResponse response, String sheetName, String activityTitle,
                       Class<?> clazz, List<?> rows) {
        String fileName = activityTitle + "-" + sheetName + "-" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=utf-8''" + encoded + ";filename=\"" + encoded + "\"");
            EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(rows);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出失败，请重试");
        }
    }
}
