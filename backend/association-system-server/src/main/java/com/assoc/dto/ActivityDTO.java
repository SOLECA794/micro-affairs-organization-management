package com.assoc.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 创建/修改活动入参（校验：截止时间晚于开始时间之前、名额为正整数由服务层复核） */
public record ActivityDTO(
        @NotBlank(message = "活动标题不能为空")
        @Size(max = 100, message = "标题最长 100 字") String title,
        Long categoryId,
        @Size(max = 255, message = "封面地址过长") String cover,
        String description,
        @NotBlank(message = "活动地点不能为空")
        @Size(max = 200, message = "地点最长 200 字") String location,
        @NotNull(message = "开始时间不能为空") LocalDateTime startTime,
        @NotNull(message = "结束时间不能为空") LocalDateTime endTime,
        @NotNull(message = "报名截止时间不能为空") LocalDateTime signupDeadline,
        @NotNull(message = "名额不能为空")
        @Min(value = 1, message = "名额必须为正整数") Integer capacity) {
}
