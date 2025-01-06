package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {

        // 需要先判断当前商品是否已经在购物车中存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart); // 直接属性拷贝即可
        // 对于当前用户的id (user_id), 在拦截器处已经获取到了  (Threadlocal)
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart); //# 虽然这里返回的是一个list, 但按照这里所设定的条件, 实际上是最多查出来一个元素的

        // 如果已经存在, 只需要将数量+1 ————> update
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            // 执行update操作
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }

        // 否则, 执行insert操作, 添加一个新的数据
        else{
            // 首先, 需要补全 cart 中其他的信息  (ShoppingCartDTO中仅包含了dish_id, setmeal_id, dish_flavor三个信息)

            // 判断当前添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            // //Long setmealId = shoppingCartDTO.getSetmealId();
            if (dishId != null) {
                // 添加的是菜品
                Dish dish = dishMapper.getByID(dishId);
                // 利用 dishId, 查到相关的信息, 并补全 shoppingCart
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else {
                // 添加的是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                // 利用 setmealId, 查到相关的信息, 并补全 shoppingCart
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            // 手动补全 shoppingCart 剩余信息
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            // 统一执行插入操作
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车 ————> 前端不需要传递任何信息, 唯一的信息 userId 也可以通过ThreadLocal获取
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {

        // userId 通过 ThreadLocal获取
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                        .userId(userId)
                        .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     * ————> 同样, 前端不需要传递任何信息, 唯一的信息 userId 也可以通过ThreadLocal获取
     */
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
