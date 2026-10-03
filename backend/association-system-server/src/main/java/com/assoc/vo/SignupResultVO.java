package com.assoc.vo;

/** 报名结果（04 文档 §5.2）：result = ACTIVE / WAITING，候补附 queueOrder */
public record SignupResultVO(Long signupId, String result, Integer queueOrder) {
}
