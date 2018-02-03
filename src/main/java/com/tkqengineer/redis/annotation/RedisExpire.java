package com.tkqengineer.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 该注解用来标记可以key是否可以过期
 *
 * @author : tengkangquan@jianyi.tech
 * @date : 2018/2/3 18:08
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RedisExpire {
}
