package com.fanjia.spring.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @Description
 * @Author fanjia <fanjia1k@163.com>
 * @Version V1.0.0
 * @Since 1.0
 * @Date 2020/4/18
 */
@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FJService {
    String value() default  "";
}
