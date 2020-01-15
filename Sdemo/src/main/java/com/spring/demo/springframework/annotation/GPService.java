package com.spring.demo.springframework.annotation;

import java.lang.annotation.*;
// 标注使用在什么地方
@Target({ElementType.TYPE})
// 表面在什么时候使用
@Retention(RetentionPolicy.RUNTIME)
// 表明注解是否可用
@Documented
public @interface GPService {

    String value() default "";
}
