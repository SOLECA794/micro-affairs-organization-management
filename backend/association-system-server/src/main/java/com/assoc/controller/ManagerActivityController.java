package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.dto.ActivityDTO;
import com.assoc.dto.ManualSigninDTO;
import com.assoc.entity.Attendance;
import com.assoc.security.RequireRole;
import com.assoc.service.ExportService;
import com.assoc.service.ManagerActivityService;
import com.assoc.service.SigninService;
import com.assoc.service.SignupService;
import com.assoc.service.StatisticsService;
import com.assoc.vo.ActivityDetailVO;
import com.assoc.vo.ActivityListVO;
import com.assoc.vo.ActivityStatisticsVO;
import com.assoc.vo.AttendanceItemVO;
import com.assoc.vo.OpenSigninVO;
import com.assoc.vo.SignupItemVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 社团端（负责人）：活动管理、名单、签到、统计、导出（04 文档 §4.3）。
 */
@Validated
@RestController
@RequestMapping("/api/manager")
@RequireRole(Constants.ROLE_MANAGER)
public class ManagerActivityController {

    private final ManagerActivityService managerActivityService;
    private final SignupService signupService;
    private final SigninService signinService;
    private final StatisticsService statisticsService;
    private final ExportService exportService;

    public ManagerActivityController(ManagerActivityService managerActivityService, SignupService signupService,
                                     SigninService signinService, StatisticsService statisticsService,
                                     ExportService exportService) {
        this.managerActivityService = managerActivityService;
        this.signupService = signupService;
        this.signinService = signinService;
        this.statisticsService = statisticsService;
        this.exportService = exportService;
    }

    /** 本社团活动列表（含草稿/待审核） */
    @GetMapping("/activities")
    public ApiResponse<PageResult<ActivityListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(managerActivityService.pageOwn(keyword, status, page, size));
    }

    /** 本社团活动详情（编辑页回填；扩展于 04 文档） */
    @GetMapping("/activities/{id}")
    public ApiResponse<ActivityDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(managerActivityService.detailOwn(id));
    }

    /** 创建活动（草稿） */
    @PostMapping("/activities")
    public ApiResponse<Long> create(@Valid @RequestBody ActivityDTO dto) {
        return ApiResponse.ok(managerActivityService.create(dto));
    }

    /** 修改草稿/被驳回活动 */
    @PutMapping("/activities/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityDTO dto) {
        managerActivityService.update(id, dto);
        return ApiResponse.ok();
    }

    /** 提交审核 */
    @PostMapping("/activities/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        managerActivityService.submit(id);
        return ApiResponse.ok();
    }

    /** 发布（需审核通过） */
    @PostMapping("/activities/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        managerActivityService.publish(id);
        return ApiResponse.ok();
    }

    /** 取消活动（已发布） */
    @PostMapping("/activities/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        managerActivityService.cancel(id);
        return ApiResponse.ok();
    }

    /** 归档（已结束） */
    @PostMapping("/activities/{id}/archive")
    public ApiResponse<Void> archive(@PathVariable Long id) {
        managerActivityService.archive(id);
        return ApiResponse.ok();
    }

    /** 报名名单（按状态筛选、分页） */
    @GetMapping("/activities/{id}/signups")
    public ApiResponse<PageResult<SignupItemVO>> signups(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(signupService.signupList(id, status, page, size));
    }

    /** 开启签到，返回签到码与二维码内容 */
    @PostMapping("/activities/{id}/signin/open")
    public ApiResponse<OpenSigninVO> openSignin(@PathVariable Long id) {
        return ApiResponse.ok(signinService.open(id));
    }

    /** 人工补签（userId） */
    @PostMapping("/activities/{id}/signin/manual")
    public ApiResponse<Attendance> manualSignin(@PathVariable Long id,
                                                @Valid @RequestBody ManualSigninDTO dto) {
        return ApiResponse.ok(signinService.manual(id, dto.userId()));
    }

    /** 统计（报名/签到/缺席/到场率） */
    @GetMapping("/activities/{id}/statistics")
    public ApiResponse<ActivityStatisticsVO> statistics(@PathVariable Long id) {
        return ApiResponse.ok(statisticsService.activityStatistics(id));
    }

    /** 导出报名名单（Excel） */
    @GetMapping("/activities/{id}/export/signups")
    public void exportSignups(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportSignups(id, response);
    }

    /** 导出签到名单（Excel） */
    @GetMapping("/activities/{id}/export/attendance")
    public void exportAttendance(@PathVariable Long id, HttpServletResponse response) {
        exportService.exportAttendance(id, response);
    }

    /** 签到名单（供签到管理页展示；扩展于 04 文档但复用名单口径） */
    @GetMapping("/activities/{id}/attendance")
    public ApiResponse<List<AttendanceItemVO>> attendance(@PathVariable Long id) {
        return ApiResponse.ok(signinService.attendanceList(id));
    }

    /** 未签到报名成功成员（补签候选；扩展接口） */
    @GetMapping("/activities/{id}/unsigned")
    public ApiResponse<List<SignupItemVO>> unsigned(@PathVariable Long id) {
        return ApiResponse.ok(signinService.unsignedActiveSignups(id));
    }
}
