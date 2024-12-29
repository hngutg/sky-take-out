package com.sky.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sky.enumeration.OperationType;

/**
 * 自定义的注解, 用于标识某个方法需要进行功能字段自动填充处理
 * (标识作用的注解, 之后, 想要使用这种切面(AOP)了, 就在对应的方法前面添加 @AutoFill 起到表示作用即可)
 * (具体的AOP操作在切面类(AutoFillAspect)中, 采用 @Pointcut("@annotation(xxx.AutoFill)")) 的形式
 */
// @Target：可使用的值定义在ElementType枚举类中，常用值如下：TYPE，类，接口 / FIELD, 成员变量 / METHOD, 成员方法
// 元注解：可以写在注解上面的注解  @Target ：指定注解能在哪里使用  @Retention ：可以理解为保留时间(生命周期)
@Target(ElementType.METHOD) // 指定当前这个注解可以作用在哪些地方 (ElementType.METHOD: 表示当前这个注解只能加在方法上)
@Retention(RetentionPolicy.RUNTIME) // 描述注解何时生效 (运行时生效)  固定写法
public @interface AutoFill {
    // 在注解中还需要指定当前的属性 ————> 通过枚举的方式来指定
    // 定义在 OperationType 内: UPDATE INSERT
    OperationType value(); // 指定数据库操作的类型(即, 仅对这两种数据库操作去做自动填充)
}
