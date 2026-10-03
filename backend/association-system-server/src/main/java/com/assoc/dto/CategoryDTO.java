package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增/修改活动分类入参 */
public record CategoryDTO(
        @NotBlank(message = "分类名不能为空")
        @Size(max = 50, message = "分类名最长 50 字") String name,
        Integer sort) {
}
