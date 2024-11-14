package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {
    // Mapper 持久层

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    // 通过注解的方式, 编写了一个sql语句   (当然, 也可以使用XML的方式来配置)
    @Select("select * from employee where username = #{username}") // 简单sql语句, 那就可以用注解的方式
                                                                   // 但如果比较复杂, 或者说是动态的sql(if, where, set等), 那就用XML的方式
    Employee getByUsername(String username);


    /**
     * 根据送入的 Employee 对象, 执行数据库的插入操作
     * @param employee
    */
    @Insert("insert into employee (name, username, password, phone, sex, id_number, create_time, update_time, create_user, update_user) " + 
            "values" + 
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

    /**
     * 分页查询 ————> 这里是动态SQL
     * @param employeePageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
    // 这里是动态SQL语句就不方便使用注解的方式了(会使用到动态标签) ————> 写到映射文件中去

    /**
     * 根据主键动态修改属性
     * @param employee
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);
    // 这里也是一个动态的SQL语句, 因此同上: 写到映射文件中去

    /**
     * 根据 ID 查询到对应的 employee 的信息
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getByID(Long id);

}
