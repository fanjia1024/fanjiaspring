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
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FJController {
    String value() default "";
}
