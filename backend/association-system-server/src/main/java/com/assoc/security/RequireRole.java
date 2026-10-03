package com.assoc.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色控制：标注在 Controller 方法（或类）上，校验当前登录角色；
 * 不匹配返回 403。未标注 = 只要求登录，不限制角色。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，如 {"STUDENT"}；多个任一满足即可 */
    String[] value();
}
