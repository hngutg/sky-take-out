package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品, 以及对应的口味
     * @param dishDTO
     */
    @Transactional // 因为涉及到对多个表的操作(菜品表, 以及口味表), 因此在这里引入了事务, 要求要么两个都操作成功, 要么都操作失败
    public void addDishWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish); // 数据拷贝 (属性名称一致才能拷贝)

        // 1. 向菜品表插入数据 （一条）
        dishMapper.insert(dish);

        // 获取insert所生成的主键值
        Long dishId = dish.getId();

        // 2. 向口味表插入数据 (一条/多条/没有)
        List<DishFlavor> flavors = dishDTO.getFlavors(); // 先拿到口味数据
        // 判断是否为空
        if (flavors != null && flavors.size() > 0) {
            // 先遍历, 赋值所属菜品的ID
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });

            // 此时, 向口味表中插入数据 ————> 实现批量插入
            dishFlavorMapper.insertBath(flavors);
        }
    }
}
