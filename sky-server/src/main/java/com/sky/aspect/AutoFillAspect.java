package com.sky.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.aspectj.lang.JoinPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * 自定义切面, 实现公共字段自动填充处理逻辑 (切面类)
 */
@Aspect // 表明当前的类不是普通的类, 而是一个AOP类
@Component // 将当前类交给IOC容器管理 (当前的类也是一个Bean)
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点 ————> 说白了就是对哪些类的哪些方法来进行拦截
     */
    // 切入点表达式  采用了 @annotation 和 execution 两种形式
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)") // 实际上 com.sky.mapper.*.*(..) 是拦截了 mapper 包下的所有类中所有的方法(所有接口/方法都会匹配到)    但实际上不用拦截这么多的
                                                                                                 // 再加一个条件, 需要当前方法添加了 AutoFill 注解
                                                                                                 //? 那为什么不直接用第二个? --> 直接annotation会扫描全部，影响效率，加上execution就会只扫描mapper里面带AutoFill注解的方法
    public void autoFillPointCut(){}

    //* @Pointcut 注解抽取出来大量的相同切入点表达式
    // ————> 这样，如果切入点改变，仅改变 autoFillPointCut() 处的 @Pointcut 注释即可, 而不需要每一个通知处都去修改切点表达式

    /**
     * 前置通知, 在通知中进行公共字段的赋值
     * @param joinpoint
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinpoint){
        //* 这里提一嘴: 其他四种通知(包括before)只能使用JoinPoint获取到目标方法运行时的相关信息，它是ProceedingJoinPoint的父类型
        //*           而只有@Around, 是通过 ProceedingJoinPoint 获取到目标方法运行时的相关信息的

        log.info("开始进行公共字段的填充");

        // 1. 获取到当前被拦截的方法上的数据库操作类型
        // 原本 joinpoint.getSignature() 所获得的结果类型为 Signature
        //# 但Signature接口提供的信息是比较一般的，例如可以获取方法名，但无法获取方法的参数类型、返回类型等更详细的信息 ————> 因此在这里使用了一个子接口 MethodSignature
        MethodSignature signature =(MethodSignature) joinpoint.getSignature(); // 方法签名对象
        // Signature 实际上是一个接口, 这里可以向下转型为 MethodSignature
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // getAnnotation(AutoFill.class), 获得到了方法上的注解对象 (即指定获取到了 AutoFill 这个注解)
        OperationType operationType = autoFill.value(); // 获得数据库操作类型  (参考AutoFill接口类或者使用时 @AutoFill(value = OperationType.UPDATE))
                                                        // 这里实际上就是获取到了 value 的值 (也即数据库操作类型)

        // 2. 获取到当前被拦截的方法的参数 ———— 和数据库相关的实体对象
        Object[] args = joinpoint.getArgs(); // 这里是获取到了所有的参数 ————> 这里是有一个设定: 如果想实现自动填充, 那默认是将那个参数放在第一位
        if(args == null || args.length == 0){
            // 判断特殊情况
            return;
        }
        Object entity = args[0]; // 所获取到的实体类型是不确定的, 因此需要用父类来接收     获得实体对象
                                 // (因为Mapper中的操作可能是插入/修改 employee/dish/Category/... , 因此需要用父类接受)

        // 3. 准备赋值的数据 ————> 还是利用 Thread （BaseContext类）
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId(); // 根据上下文, 获取到当前的用户ID

        // 4. 根据不同的操作类型, 对对应的属性来赋值 ————> 通过反射来赋值
        if(operationType == OperationType.INSERT){
            // 插入操作, 需要为四个公共字段赋值
            // 需要获得相应的 set 方法来进行赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                // 函数里面所指定的是: 方法名称, 所需要的参数类型(利用反射的方式获取到, 这里实际上所获取的事一个Class对象)
                // 因为这里实体类型不确定，没有办法直接调set方法，但是通过反射可以百分百确定调的set方法就是当前实体类的set方法

                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                //* 上述即为获取到 entity 所属类别(entity.getClass())的 get/set 方法(.getDeclaredMethod())

                // 利用反射来为对象属性赋值 ————> 即, 利用所获得的实体对象, 调用获得到的 set 方法, 实现属性赋值
                setCreateTime.invoke(entity, now); //* Method.invoke(obj, args) ————> 即相当于执行 obj.Method(args)
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
                //! 整个过程就是对反射的应用, 建议回过头去复习一下反射

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }else if(operationType == OperationType.UPDATE){
            // 更新操作, 仅需两个公共字段的赋值
            try {
                // 函数里面所指定的是: 方法名称, 所需要的参数类型(利用反射的方式获取到, 这里实际上所获取的事一个Class对象)
                // 因为这里实体类型不确定，没有办法直接调set方法，但是通过反射可以百分百确定调的set方法就是当前实体类的set方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 利用反射来为对象属性赋值 ————> 即, 利用所获得的实体对象, 调用获得到的 set 方法, 实现属性赋值
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
                //! 整个过程就是对反射的应用, 建议回过头去复习一下反射

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
