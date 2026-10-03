package com.assoc.vo;

import java.time.LocalDateTime;

/** 开启签到出参（04 文档 §5.3） */
public record OpenSigninVO(String token, LocalDateTime expiresAt, String qrcodeUrl) {
}
