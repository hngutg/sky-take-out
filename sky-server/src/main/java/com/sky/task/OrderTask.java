package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类, 定时处理订单状态
 */
@Component // 表示当前的类也需要实例化, 并交给Spring容器管理
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟触发一次
    public void processTimeoutOrder() {
        log.info("定时处理超时订单: {}", LocalDateTime.now());

        // 先把超时的订单查出来
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));// 当前时间 - 15分钟

        if(ordersList != null && ordersList.size() > 0) {
            for(Orders order : ordersList) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时, 自动取消");
                order.setCancelTime(LocalDateTime.now());

                // 之后, 再更新回去
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理一直处于"派送中"状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨一点触发一次即可
    public void processDeliveryOrder() {
        log.info("处理一直处于\"派送中\"状态的订单: {}", LocalDateTime.now());

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));// 查昨天的一直在派送中的订单

        if (ordersList != null && ordersList.size() > 0) {
            for(Orders order : ordersList) {
                order.setStatus(Orders.COMPLETED);
                // 之后, 再更新回去
                orderMapper.update(order);
            }
        }
    }
}
