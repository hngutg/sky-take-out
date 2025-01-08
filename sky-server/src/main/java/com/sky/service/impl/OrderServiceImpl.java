package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO 传入的DTO信息
     * @return 返回给前端的VO信息
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 先处理各种业务异常 (比如: 地址为空/购物车数据为空)
        //  地址为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 此时, 地址不在数据库中, 认为当前地址异常, 抛出异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //  购物车为空
        Long userId = BaseContext.getCurrentId(); // 获取当前用户ID, 用于查询购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart); // 获取到购物车数据
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            // 此时, 同样抛出异常  (购物车数据为空，不能下单)
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 数据无异常, 再执行下单操作

        // 1. 向订单表插入1条数据
        Orders orders = new Orders(); // Orders实体类 (其中有很多很多的数据)
        BeanUtils.copyProperties(ordersSubmitDTO, orders); // 直接实现属性拷贝
        // 其余的一些属性就需要自己设置了
        orders.setOrderTime(LocalDateTime.now()); // 设置订单的创建时间
        orders.setPayStatus(Orders.UN_PAID); // 支付状态设置为: 未支付
        orders.setStatus(Orders.PENDING_PAYMENT); // 订单状态设置为: 代付款
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 设置订单号 (当前系统时间的时间戳)
        orders.setPhone(addressBook.getPhone()); // 前面查出来的 addressBook 中是包含手机号的
        orders.setConsignee(addressBook.getConsignee()); // 设置收货人
        orders.setUserId(userId); // 设置当前订单所属的用户 (通过ThreadLocal获取)
        // 之后, 执行插入
        orderMapper.insert(orders); // 这里是需要返回主键值的 (返回到orders内)

        // 2. 向订单明细表中插入n条数据 ————> n由购物车中的数据数量来决定
        List<OrderDetail> orderDetailList = new ArrayList<>(); // 用一个list保存所有的订单明细数据, 最终实现批量插入
        for (ShoppingCart cart : shoppingCartList) {
            // 购物车中的每个元素都要执行一次插入
            OrderDetail orderDetail = new OrderDetail(); // 订单明细  (包装插入信息)
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 手动补充订单ID信息
            orderDetailList.add(orderDetail);
        }
        // 实现批量插入
        orderDetailMapper.insertBatch(orderDetailList);

        // 3. 下单后, 清空用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 4. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //# 跳过微信支付功能
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 支付成功后, 通过websocket向客户端浏览器推送成功消息
        Map map = new HashMap();
        map.put("type", 1); // 1表示来单提醒, 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号是: " + outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }
}
