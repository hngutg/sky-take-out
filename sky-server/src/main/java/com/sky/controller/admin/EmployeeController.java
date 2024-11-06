package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口") // 可以给类添加Swagger的注解, 使得生成的接口文档的可读性更好
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties; // JwtProperties是一个配置属性类

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "员工登录") // 同样, 也可以给类中的方法添加Swagger的注解, 使得生成的接口文档的可读性更好
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        // DTO对象(EmployeeLoginDTO)中包含的就是从前端传过来的信息(用户名、密码)
        // 调用 sky-take-out-full\sky-take-out\sky-server\src\main\java\com\sky\service\impl\EmployeeServiceImpl.java 中的 login 函数, 执行员工的登录过程
        Employee employee = employeeService.login(employeeLoginDTO);
        // 如果登录成功(在数据库中找到了对应的员工, 且没有被禁用)
        // ————> 返回对应的员工对象

        //登录成功后，为前端生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        // 生成了 jwt令牌 之后, 就需要进行封装, 相应给前端页面     (各种前端页面需要的信息)
        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build(); // 最终调用 build() 方法, 把这个对象构建好
        // 利用这种方式(builder)来构建对象的前提:
        // ———— 在原本的类 (EmployeeLoginVO) 中, 需要添加 @Builder 注解

        return Result.success(employeeLoginVO); // 将这个结果再进行封装, 封装到 Result 类中
        // 后端给前端相应的数据统一使用 Result 这种形式
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工登出") // 同样, 也可以给类中的方法添加Swagger的注解, 使得生成的接口文档的可读性更好
    public Result<String> logout() {
        return Result.success();
    }

}
