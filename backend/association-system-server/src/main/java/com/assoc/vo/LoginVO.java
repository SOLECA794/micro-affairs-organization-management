package com.assoc.vo;

/** 登录出参：Token 与用户信息（含角色） */
public record LoginVO(String token, UserVO user) {
}
