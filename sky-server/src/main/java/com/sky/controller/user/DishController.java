package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        //# 利用redis减少对于数据库的访问次数
        // ————> redis中存储的数据相当于存在服务端, 而并非客户端   (仅仅减少数据库访问次数, 而不减少网络请求次数)

        // 先构造redis中的key, 规则为: dish_ + 分类id
        String key = "dish_" + categoryId;

        // 1. 查询redis中是否存在菜品数据   (value在redis中被设定为String)
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);

        // 2. 如果存在, 直接返回, 无需访问数据库
        if (list != null && list.size() > 0) {
            return Result.success(list);
        }

        // 3. 如果不存在, 那就需要查询数据库, 并将查询的数据放入到redis中  (也即后续的代码)
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        list = dishService.listWithFlavor(dish);
        // 将查询的数据放入到redis中
        redisTemplate.opsForValue().set(key, list);

        return Result.success(list);
    }
}
