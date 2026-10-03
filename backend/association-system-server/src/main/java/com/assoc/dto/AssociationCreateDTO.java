package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 管理员创建社团入参 */
public record AssociationCreateDTO(
        @NotBlank(message = "社团名称不能为空")
        @Size(max = 100, message = "名称最长 100 字") String name,
        @NotBlank(message = "社团编号不能为空")
        @Size(max = 30, message = "编号最长 30 字") String code,
        @Size(max = 50, message = "分类最长 50 字") String category,
        @NotNull(message = "负责人不能为空") Long leaderUserId,
        String description) {
}
