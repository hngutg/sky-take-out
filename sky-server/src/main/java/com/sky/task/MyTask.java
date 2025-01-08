package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自定义定时任务类
 */
@Component // 表示当前的类也需要实例化, 并交给Spring容器管理
@Slf4j
public class MyTask {

    /**
     * 一个简单的定时任务, 每隔5秒触发一次
     */
    // @Scheduled(cron = "0/5 * * * * ?") // 指定任务的触发时机(由cron表达式指定)
    public void executeTask(){
        log.info("定时任务执行: {}", new Date());
    }
}
