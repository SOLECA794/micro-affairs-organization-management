package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.dto.ManagerAssociationUpdateDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.ManagerAssociationService;
import com.assoc.service.StatisticsService;
import com.assoc.vo.AssociationVO;
import com.assoc.vo.ManagerOverviewVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社团端：本社团资料与工作台统计（04 文档 §4.3 + 05 文档工作台依赖的概览接口）。
 */
@RestController
@RequestMapping("/api/manager")
@RequireRole(Constants.ROLE_MANAGER)
public class ManagerAssociationController {

    private final ManagerAssociationService managerAssociationService;
    private final StatisticsService statisticsService;

    public ManagerAssociationController(ManagerAssociationService managerAssociationService,
                                        StatisticsService statisticsService) {
        this.managerAssociationService = managerAssociationService;
        this.statisticsService = statisticsService;
    }

    /** 本社团资料 */
    @GetMapping("/association")
    public ApiResponse<AssociationVO> getOwn() {
        return ApiResponse.ok(managerAssociationService.getOwn());
    }

    /** 维护本社团资料 */
    @PutMapping("/association")
    public ApiResponse<AssociationVO> updateOwn(@Valid @RequestBody ManagerAssociationUpdateDTO dto) {
        return ApiResponse.ok(managerAssociationService.updateOwn(dto));
    }

    /** 工作台统计概览（05 文档 §2 工作台页面依赖 manager/statistics） */
    @GetMapping("/statistics")
    public ApiResponse<ManagerOverviewVO> overview() {
        return ApiResponse.ok(statisticsService.managerOverview());
    }
}
