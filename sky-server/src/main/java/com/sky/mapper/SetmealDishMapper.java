package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品的ID来查询相关的套餐ID ————> 需要用动态SQL拼接 dishIds
     * @param dishIds
     * @return
     */
    public List<Long> getSetmealIdsByDishId(List<Long> dishIds);
}
