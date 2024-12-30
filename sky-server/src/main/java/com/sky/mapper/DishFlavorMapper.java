package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {


    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBath(List<DishFlavor> flavors);

    /**
     * 根据菜品的ID来删除对应的口味
     * @param dishId
     */
    @Delete("delete from dish_flavor where  dish_id = #{dishId}")
    void deleteByDishID(Long dishId);

    /**
     * 根据菜品的ID集合来删除对应的口味
     * @param dishIDs
     */
    void deleteByDishIDs(List<Long> dishIDs);

    /**
     * 根据菜品的ID来查询对应的口味
     * @param dishID
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{dishID}")
    List<DishFlavor> getByDishID(Long dishID);
}
