package com.assoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义配置项（assoc.*），支持环境变量覆盖。
 */
@ConfigurationProperties(prefix = "assoc")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Qrcode qrcode = new Qrcode();
    private Front front = new Front();

    public static class Jwt {
        /** HS256 密钥，长度不得少于 32 字节；生产通过 JWT_SECRET 注入 */
        private String secret;
        /** Token 有效期（小时） */
        private long expireHours = 24;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpireHours() {
            return expireHours;
        }

        public void setExpireHours(long expireHours) {
            this.expireHours = expireHours;
        }
    }

    public static class Qrcode {
        /** 二维码内容基地址（指向后端扫码落地页），生产通过环境变量注入 */
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Front {
        /** 扫码落地跳转的前端基地址 */
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Qrcode getQrcode() {
        return qrcode;
    }

    public void setQrcode(Qrcode qrcode) {
        this.qrcode = qrcode;
    }

    public Front getFront() {
        return front;
    }

    public void setFront(Front front) {
        this.front = front;
    }
}
