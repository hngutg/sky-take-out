package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        // DTO对象中包含的就是从前端传过来的信息(用户名、密码)
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        // 获取到输入的用户名和密码

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username); // 调用 Mapper 层来查询数据库, 以用户名为条件

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND); // 这是一个自定义的异常类
            // 在 sky-common 中的 exception 中定义
        }

        //* 密码比对: 将前端送过来的明文密码先进行 md5 加密, 之后再去与后端的数据库进行比较
        // ————> 因为后端数据库中实际上也是存储的密码的加密结果   (md5 加密形式, 无法实现解密, 因此最终数据库中也存储的是加密后的结果)
        password = DigestUtils.md5DigestAsHex(password.getBytes()); // 这里将前端送过来的 password 通过MD5进行转化
        if (!password.equals(employee.getPassword())) {
            // 如果密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
    */
    public void addNewEmployee(EmployeeDTO employeeDTO) {
        // 获取当前线程的ID
        // System.out.println("当前线程的ID: " + Thread.currentThread().getId());

        // 这里传过来的是一个 DTO 类型, 为了方便封装前端提交过来的数据
        // 但是传给持久层的时候, 还是建议使用实体类 (因为在Mapper层实行数据库操作的时候, 那些DTO中没有包含的属性可能也会用上的)
        Employee employee = new Employee();
        // 直接使用对象属性拷贝的方式, 快速将数据拷贝到 employee 中
        BeanUtils.copyProperties(employeeDTO, employee); // 从 employeeDTO 中拷贝到 employee (从源(第一个参数)拷贝到目标(第二个参数))
                                                         // 前提: 属性名必须一致
        // 而 employee 中的其他属性, 就需要自己手动设置了

        // 设置账号状态, 默认正常状态
        employee.setStatus(StatusConstant.ENABLE);
        // 设置密码:  (进行MD5加密后再进行存储)
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 创建时间/修改时间:   使用系统时间即可
        // employee.setCreateTime(LocalDateTime.now());
        // employee.setUpdateTime(LocalDateTime.now());

        // 当前记录的创建人ID和修改人ID:  (当前登录用户的ID)
        //* 利用 ThreadLocal 中存储的上下文, 获取到当前登录用户的ID
        // employee.setCreateUser(BaseContext.getCurrentId());
        // employee.setUpdateUser(BaseContext.getCurrentId());

        //! 在设置了公共属性的赋值之后, 就不再需要在这里单独赋值了

        // 之后, 插入数据
        //* 调用持久层 EmployeeMapper 实现插入
        employeeMapper.insert(employee);
    }

    /**
     * 采用了pageHelper插件后, 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 底层实际上是基于 MySQL 中的 limit 关键字来实现的查询: select * from employee limit 0,10

        //* 利用 pagehelper 来实现动态的分页
        // 开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize()); // 给进去页码和每页的大小
        // 这个插件的底层实际上是基于 mybatis 的拦截器来实现的 ————> 将后面的sql语句进行动态拼接
        //* 底层实际上也是基于 ThreadLocal() 实现的 ————> 利用 ThreadLocal 存储上下文, 实现对于后续方法的影响

        // 调用持久层 EmployeeMapper 实现查询
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO); // PageHelper 要求的后面这个方法的返回值是固定的: Page
        // 当前所获取到的这个List集合, 实际上是分页查询结果的封装类(Page类型)

        // 再将 Page 转换为 PageResult, 即封装为 PageResult 对象
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        // 封装到对象中去
        return new PageResult(total, records);
    }

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //* 本质上是对SQL的一个修改操作
        // update employee set status = ? where id = ?
        // ————> 这里想要的事动态更新, 即这个update操作不仅仅限于上述所提及的两个参数

        // 构建一个 Employee 对象
        // Employee employee = new Employee();
        // employee.setStatus(status);
        // employee.setId(id);
        // 构建方式也可以使用 builder 来构建, 可以选择构造时需要的参数 ————> Employee 上有受到 @Builder 的修饰
        Employee employee = Employee.builder()  // 这里获取到了 Builder 的对象
                        .status(status) // 指定参数的方法和参数名称是一致的
                        .id(id)
                        .build(); // 创建 Employee 对象

        // 实现动态更新
        employeeMapper.update(employee);
    }

    /**
     * 据ID查询员工信息
     * @param id
     * @return
     */
    public Employee getByID(Long id) {
        //* 调用持久层 EmployeeMapper 实现查询
        Employee employee = employeeMapper.getByID(id); // 期望查出来一个 Employee 对象
        // 简单处理一下, 不希望前端能看到密码
        employee.setPassword("****");

        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO) {
        
        // 这里就可以利用前面在mapper中所构建的 update 动态更新方法了

        // 注意, update 更新所需要的数据类型为 Employee, 需要类型转换
        // 对象的属性拷贝
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee); // 直接使用对象属性拷贝的方式, 快速将数据拷贝到 employee 中

        // 再单独设置一下修改人和修改时间
        // employee.setUpdateTime(LocalDateTime.now());
        // 可以像之前一样, 利用Thread保存上下文信息, 获取到当前的用户id
        // employee.setUpdateUser(BaseContext.getCurrentId()); // 在拦截器 JwtTokenAdminInterceptor 中获取到的
                                                            // 每次在拦截器进行jwt校验的时候, 都会保存当前的用户ID到Thread中去
        //! 在设置了公共属性的赋值之后, 就不再需要在这里单独赋值了

        // 调用 Mapper 的 update 方法, 实现动态更新
        employeeMapper.update(employee);
    }
}