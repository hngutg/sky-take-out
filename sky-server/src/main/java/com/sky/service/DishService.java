package com.sky.service;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

public interface DishService {

    /**
     * 新增菜品, 以及对应的口味数据
     * @param dish
     */
    public void addDishWithFlavor(DishDTO dish);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);
}
