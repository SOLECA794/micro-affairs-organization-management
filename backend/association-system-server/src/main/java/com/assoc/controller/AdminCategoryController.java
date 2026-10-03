package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.common.Constants;
import com.assoc.dto.CategoryDTO;
import com.assoc.security.RequireRole;
import com.assoc.service.AdminCategoryService;
import com.assoc.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：活动分类管理（04 文档 §4.4）。
 * 说明：GET 供三端下拉与分类页使用，登录即可访问；增删改仅 ADMIN。
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    /** 分类列表（全量，按 sort 排序） */
    @GetMapping
    public ApiResponse<List<CategoryVO>> list() {
        return ApiResponse.ok(adminCategoryService.listAll());
    }

    /** 新增分类 */
    @RequireRole(Constants.ROLE_ADMIN)
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CategoryDTO dto) {
        return ApiResponse.ok(adminCategoryService.create(dto));
    }

    /** 修改分类 */
    @RequireRole(Constants.ROLE_ADMIN)
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        adminCategoryService.update(id, dto);
        return ApiResponse.ok();
    }

    /** 删除分类 */
    @RequireRole(Constants.ROLE_ADMIN)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return ApiResponse.ok();
    }
}
