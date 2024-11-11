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

        //密码比对
        // todo: 后期需要进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes()); // 这里将前端送过来的 password 通过MD5进行转化
        if (!password.equals(employee.getPassword())) {
            //密码错误
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
    public void save(EmployeeDTO employeeDTO) {
        // 获取当前线程的ID
        System.out.println("当前线程的ID: " + Thread.currentThread().getId());

        // 这里传过来的是一个 DTO 类型, 为了方便封装前端提交过来的数据
        // 但是传给持久层的时候, 还是建议使用实体类
        Employee employee = new Employee();
        // 直接使用对象属性拷贝的方式, 快速将数据拷贝到 employee 中
        BeanUtils.copyProperties(employeeDTO, employee); // 从 employeeDTO 中拷贝到 employee (从源(第一个餐宿)拷贝到目标(第二个参数))
                                                         // 前提: 属性名必须一致
        // 而 employee 中的其他属性, 就需要自己手动设置了

        // 设置账号状态, 默认正常状态
        employee.setStatus(StatusConstant.ENABLE);
        // 设置密码:  (进行MD5加密后再进行存储)
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        // 创建时间/修改时间:   使用系统时间即可
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        // 当前记录的创建人ID和修改人ID:  (当前登录用户的ID)
        //* 利用 ThreadLocal 中存储的上下文, 获取到当前登录用户的ID
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        // 之后, 插入数据
        //* 调用持久层 EmployeeMapper 实现插入
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 底层实际上是基于 MySQL 中的 limit 关键字来实现的查询: select * from employee limit 0,10

        // 利用 pagehelper 来实现动态的分页
        // 开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize()); // 给进去页码和每页的大小
        // 这个插件的底层实际上是基于 mybatis 的拦截器来实现的 ————> 将后面的sql语句进行动态拼接
        //* 底层实际上也是基于 ThreadLocal() 实现的 ————> 利用 ThreadLocal 存储上下文, 实现对于后续方法的影响

        //* 调用持久层 EmployeeMapper 实现查询
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO); // PageHelper 要求的后面这个方法的返回值是固定的: Page

        // 再将 Page 转换为 PageResult
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        // 封装到对象中去
        return new PageResult(total, records);
    }

}
