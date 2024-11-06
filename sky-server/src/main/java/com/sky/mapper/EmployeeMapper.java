package com.sky.mapper;

import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    // 通过注解的方式, 编写了一个sql语句   (当然, 也可以使用XML的方式来配置)
    @Select("select * from employee where username = #{username}") // 简单sql语句, 那就可以用注解的方式
                                                                   // 但如果比较复杂, 或者说是动态的sql(if, where, set等), 那就用XML的方式
    Employee getByUsername(String username);

}
