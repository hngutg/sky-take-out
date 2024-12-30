package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT) // 利用AOP, 实现对于公共字段的填充
    void insert(Dish dish);

    /**
     * 菜品分页查询 ————> 动态SQL
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据ID查询菜品, 并返回查询的Dish信息
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish getByID(Long id);

    /**
     * 根据主键删除菜品数据
     * @param id
     */
    @Delete("delete from dish where id = #{id}")
    void deleteByID(Long id);
}
