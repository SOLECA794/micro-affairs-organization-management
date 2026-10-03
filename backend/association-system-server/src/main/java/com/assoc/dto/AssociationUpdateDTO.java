package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 管理员维护社团入参（含负责人、状态） */
public record AssociationUpdateDTO(
        @NotBlank(message = "社团名称不能为空")
        @Size(max = 100, message = "名称最长 100 字") String name,
        @Size(max = 50, message = "分类最长 50 字") String category,
        @NotNull(message = "负责人不能为空") Long leaderUserId,
        @NotNull(message = "状态不能为空") Integer status,
        String description) {
}
