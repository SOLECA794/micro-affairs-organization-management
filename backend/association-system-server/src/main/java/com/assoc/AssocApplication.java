package com.assoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社团活动报名与签到系统 后端服务入口。
 */
@EnableScheduling
@SpringBootApplication
public class AssocApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssocApplication.class, args);
    }
}
