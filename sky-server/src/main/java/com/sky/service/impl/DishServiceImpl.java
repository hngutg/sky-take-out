package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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

    @Autowired
    private SetmealDishMapper setmealDishMapper;

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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 基于PageHelper插件, 来实现分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 注意, 返回值的反应应为 DishVO
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional // 涉及对多个表的操作, 应当保证一致性 ————> 开启事务
    public void deleteBatch(List<Long> ids) {

        // 先判断菜品能否删除
        // 1. 判断当前菜品是否存在 起售中 的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getByID(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                // 如果还在卖, 那就抛异常
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 2. 判断当前菜品是否被套餐关联 (查关系表)
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            // 说明查到了这种套餐, 不允许删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        // for (Long id : ids) {
        //     dishMapper.deleteByID(id);
        // // 删除菜品关联的口味数据
        // dishFlavorMapper.deleteByDishID(id);
        // }

        // 批量删除菜品
        dishMapper.deleteByIDs(ids);
        dishFlavorMapper.deleteByDishIDs(ids);
    }

    /**
     * 根据ID来查询菜品以及对应的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 一共是要查两个表的

        // 1. 根据ID查询菜品数据
        Dish dish = dishMapper.getByID(id);

        // 2. 根据菜品ID查询对应的口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishID(id);

        // 3. 将查询结果封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据ID修改菜品的基本信息, 以及对应的口味信息
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        // 这里可能会操作到多张表

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改菜品表的基本信息
        dishMapper.update(dish); // 传入dish, 从设计层面来说, 更加合理

        // 注意: 这里对于口味的操作实际上是十分复杂的
        //? 如何对口味数据进行修改呢?
        // ————> 先把当前菜品原本关联的口味数据全部删除, 之后再按照传来的信息, 重新执行插入
        dishFlavorMapper.deleteByDishID(dishDTO.getId()); // 先把当前菜品原本关联的口味数据全部删除
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 之后再按照传来的信息, 重新执行插入
        if (flavors != null && flavors.size() > 0) {
            // 先遍历, 赋值所属菜品的ID
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });

            // 此时, 向口味表中插入数据 ————> 实现批量插入
            dishFlavorMapper.insertBath(flavors);
        }
    }
}
