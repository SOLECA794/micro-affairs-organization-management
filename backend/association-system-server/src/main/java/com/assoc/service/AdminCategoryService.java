package com.assoc.service;

import com.assoc.common.BusinessException;
import com.assoc.common.Constants;
import com.assoc.common.ErrorCode;
import com.assoc.dto.CategoryDTO;
import com.assoc.entity.Activity;
import com.assoc.entity.ActivityCategory;
import com.assoc.mapper.ActivityCategoryMapper;
import com.assoc.mapper.ActivityMapper;
import com.assoc.security.UserContext;
import com.assoc.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端：活动分类增删改与排序。
 */
@Service
public class AdminCategoryService {

    private final ActivityCategoryMapper categoryMapper;
    private final ActivityMapper activityMapper;
    private final OperationLogService operationLogService;

    public AdminCategoryService(ActivityCategoryMapper categoryMapper, ActivityMapper activityMapper,
                                OperationLogService operationLogService) {
        this.categoryMapper = categoryMapper;
        this.activityMapper = activityMapper;
        this.operationLogService = operationLogService;
    }

    /** 分类全量列表（按 sort 排序；供管理端与活动创建下拉使用） */
    public List<CategoryVO> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ActivityCategory>()
                        .orderByAsc(ActivityCategory::getSort)
                        .orderByAsc(ActivityCategory::getId))
                .stream()
                .map(category -> new CategoryVO(category.getId(), category.getName(), category.getSort()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(CategoryDTO dto) {
        ActivityCategory category = new ActivityCategory();
        category.setName(dto.name());
        category.setSort(dto.sort() == null ? 0 : dto.sort());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUPLICATE, "分类名已存在");
        }
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "新增分类",
                "分类: " + category.getName());
        return category.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryDTO dto) {
        ActivityCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        category.setName(dto.name());
        category.setSort(dto.sort() == null ? category.getSort() : dto.sort());
        try {
            categoryMapper.updateById(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.DUPLICATE, "分类名已存在");
        }
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "修改分类",
                "分类: " + category.getName());
    }

    /** 删除分类：被活动引用时拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ActivityCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        long referenced = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCategoryId, id));
        if (referenced > 0) {
            throw new BusinessException(ErrorCode.STATE_NOT_ALLOWED, "分类已被活动引用，无法删除");
        }
        categoryMapper.deleteById(id);
        operationLogService.record(UserContext.userId(), Constants.MODULE_ADMIN, "删除分类",
                "分类: " + category.getName());
    }
}
