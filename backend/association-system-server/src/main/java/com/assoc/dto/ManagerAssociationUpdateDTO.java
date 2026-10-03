package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 负责人维护本社团资料入参（不含状态与负责人变更） */
public record ManagerAssociationUpdateDTO(
        @NotBlank(message = "社团名称不能为空")
        @Size(max = 100, message = "名称最长 100 字") String name,
        @Size(max = 50, message = "分类最长 50 字") String category,
        String description) {
}
