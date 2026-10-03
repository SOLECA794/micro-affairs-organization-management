package com.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 维护个人资料入参（姓名、手机号、头像） */
public record ProfileUpdateDTO(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名过长") String realName,
        @Size(max = 20, message = "手机号过长") String phone,
        @Size(max = 255, message = "头像地址过长") String avatar) {
}
