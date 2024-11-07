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
