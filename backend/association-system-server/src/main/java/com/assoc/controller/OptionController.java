package com.assoc.controller;

import com.assoc.common.ApiResponse;
import com.assoc.entity.ActivityCategory;
import com.assoc.entity.Association;
import com.assoc.mapper.ActivityCategoryMapper;
import com.assoc.mapper.AssociationMapper;
import com.assoc.vo.OptionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开下拉选项（实现期扩展）：社团与分类的 {id, name} 列表。
 * 公开（匿名可访问）、无 PII：仅返回 id 与名称，用于学生端活动筛选。
 */
@RestController
@RequestMapping("/api/options")
public class OptionController {

    private final AssociationMapper associationMapper;
    private final ActivityCategoryMapper categoryMapper;

    public OptionController(AssociationMapper associationMapper, ActivityCategoryMapper categoryMapper) {
        this.associationMapper = associationMapper;
        this.categoryMapper = categoryMapper;
    }

    /** 启用状态的社团选项 [{id, name}] */
    @GetMapping("/associations")
    public ApiResponse<List<OptionVO>> associationOptions() {
        List<OptionVO> list = associationMapper.selectList(new LambdaQueryWrapper<Association>()
                        .eq(Association::getStatus, 1)
                        .orderByAsc(Association::getName))
                .stream()
                .map(a -> new OptionVO(a.getId(), a.getName()))
                .toList();
        return ApiResponse.ok(list);
    }

    /** 活动分类选项 [{id, name}]（按 sort 升序） */
    @GetMapping("/categories")
    public ApiResponse<List<OptionVO>> categoryOptions() {
        List<OptionVO> list = categoryMapper.selectList(new LambdaQueryWrapper<ActivityCategory>()
                        .orderByAsc(ActivityCategory::getSort))
                .stream()
                .map(c -> new OptionVO(c.getId(), c.getName()))
                .toList();
        return ApiResponse.ok(list);
    }
}
