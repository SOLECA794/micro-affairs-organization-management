package com.assoc.controller;

import com.assoc.config.AppProperties;
import com.assoc.entity.Activity;
import com.assoc.entity.SigninCode;
import com.assoc.mapper.ActivityMapper;
import com.assoc.mapper.SigninCodeMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 扫码落地页（02 文档 §5.5：二维码内容为签到接口地址）。
 * GET /api/signin/qrcode?token=xxx → 302 跳转到前端活动详情页并携带签到 Token，
 * 前端自动提交 POST /api/signin/qrcode 完成签到。
 */
@RestController
public class SigninLandingController {

    private final SigninCodeMapper signinCodeMapper;
    private final ActivityMapper activityMapper;
    private final AppProperties properties;

    public SigninLandingController(SigninCodeMapper signinCodeMapper, ActivityMapper activityMapper,
                                   AppProperties properties) {
        this.signinCodeMapper = signinCodeMapper;
        this.activityMapper = activityMapper;
        this.properties = properties;
    }

    @GetMapping("/api/signin/qrcode")
    public void landing(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        SigninCode code = signinCodeMapper.selectByToken(token);
        boolean valid = code != null && code.getStatus() != null && code.getStatus() == 1
                && code.getExpiresAt() != null && LocalDateTime.now().isBefore(code.getExpiresAt());
        String base = properties.getFront().getBaseUrl();
        if (!valid) {
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write("<!doctype html><html lang=\"zh\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>签到码无效</title></head><body style=\"font-family:sans-serif;text-align:center;"
                    + "padding-top:80px;color:#333\"><h2>签到码无效或已过期</h2>"
                    + "<p>请联系活动负责人重新开启签到。</p></body></html>");
            return;
        }
        Long activityId = code.getActivityId();
        Activity activity = activityMapper.selectById(activityId);
        String target = base + "/activities/" + activityId + "?signinToken=" + token;
        response.sendRedirect(target);
    }
}
