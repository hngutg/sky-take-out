package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLIntegrityConstraintViolationException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //* 有关方法重载: 如果一个方法的参数类型是某个类的子类，而另一个方法的参数类型是该类的父类，这样的重载本身是合法的
    // 这样重载的方法选择如下:
    // ————> 当你调用一个重载方法时，Java 编译器会根据传入参数的静态类型（即编译时类型）来选择合适的方法。
    // ————> 如果传入的参数是子类的实例，子类版本的方法将被调用。
    // ————> 如果传入的参数是父类的实例，父类版本的方法将被调用。   (注意: 如果传入的参数是一个子类对象，但其被引用为父类类型（向上转型），那么父类版本的方法将被调用。)

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        // 在这里, 负责捕获所有的异常
        // BaseException 实际上是所有自定以的异常类的父类
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage()); // 给前端页面返回一个结果
    }

    /**
     * 处理 SQL 异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        // Duplicate entry 'zhangsan' for key 'employee.idx_username'
        // 首先, 根据 getMessage() 方法, 获取异常信息
        String exMessage = ex.getMessage();
        // 之后, 判断 exMessage 中是否包含 "Duplicate entry"
        if(exMessage.contains("Duplicate entry")){
            // 此时, 返回信息: 当前元素已经存在 ————> 动态信息
            String[] splitStrings = exMessage.split(" ");
            String userName = splitStrings[2];
            String msg = userName + MessageConstant.ALREADY_EXISTS;
            // 再将这个信息进行封装
            return Result.error(msg);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
