package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 活动审核驳回入参：意见必填 */
public record AuditRejectDTO(
        @NotBlank(message = "驳回意见不能为空")
        @Size(max = 500, message = "意见最长 500 字") String comment) {
}
