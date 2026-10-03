package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.common.PageResult;
import com.assoc.dto.AssociationCreateDTO;
import com.assoc.dto.AssociationUpdateDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.AdminAssociationService;
import com.assoc.vo.AssociationVO;
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

/**
 * 管理端：社团管理（04 文档 §4.4）。
 */
@RestController
@RequestMapping("/api/admin/associations")
@RequireRole(Constants.ROLE_ADMIN)
public class AdminAssociationController {

    private final AdminAssociationService adminAssociationService;

    public AdminAssociationController(AdminAssociationService adminAssociationService) {
        this.adminAssociationService = adminAssociationService;
    }

    /** 社团列表 */
    @GetMapping
    public ApiResponse<PageResult<AssociationVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(Constants.MAX_SIZE) int size) {
        return ApiResponse.ok(adminAssociationService.page(keyword, status, page, size));
    }

    /** 创建社团 */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody AssociationCreateDTO dto) {
        return ApiResponse.ok(adminAssociationService.create(dto));
    }

    /** 维护社团（含负责人、状态） */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody AssociationUpdateDTO dto) {
        adminAssociationService.update(id, dto);
        return ApiResponse.ok();
    }
}
