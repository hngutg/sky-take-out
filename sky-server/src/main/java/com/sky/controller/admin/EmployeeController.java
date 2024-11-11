package com.sky.controller.admin;

import com.fasterxml.jackson.annotation.JsonTypeInfo.None;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.server.PathParam;

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
     * @return  注解, 说明该方法有返回值
     */
    @PostMapping("/logout")
    @ApiOperation("员工登出") // 同样, 也可以给类中的方法添加Swagger的注解, 使得生成的接口文档的可读性更好
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 创建新的员工对象
     * 提交过来的是一个 EmployeeDTO 的对象
     * 
     * @return 注解, 说明该方法有返回值
    */
    // 这个方法的请求方式为 Post, 因此需要在这里添加一个注解:
    @PostMapping  // 这里不用给路径, 因为在所属的类 EmployeeController 中已经添加过了
    @ApiOperation("新增员工")
    public Result addNewEmployee(@RequestBody EmployeeDTO employeeDTO){
        // 由于接收的是 json 形式的数据, 因此需要添加这个 @RequestBody 注解

        // 获取当前线程的ID
        System.out.println("当前线程的ID: " + Thread.currentThread().getId());

        log.info("新增员工: {}", employeeDTO);

        // 调用 EmployeeService 中的方法来真的完成更新操作
        employeeService.save(employeeDTO);

        return Result.success();
    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page") // 这一个get形式的请求, 且注意路径
    @ApiOperation("员工分页查询")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        // 注意, 前端提交过来的数据不是 json 类型的, 而是 query 类型   (不用添加那个 @RequestBody 注解) ————> 会自动实现封装, 将前端提交的数据封装为 EmployeePageQueryDTO 类型

        log.info("员工分页查询, 参数为: {}", employeePageQueryDTO);

        // 调用 Service 来实际进行查询
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO); // 利用 EmployeeService 中的 pageQuery 方法来进行查询
                                                                                 // 希望返回一个 PageResult 的对象

        return Result.success(pageResult);
    }

    //! Result后面所跟的泛型: 如果是查询操作, 那后端返回中 Result 的 dtat 内一般是会带有数据的, 因此需要指定泛型
    //!                      而如果不是查询操作, 那后端只需要反一个 code 给前端即可, 用不到 data, 因此也不用指定泛型
    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}") // 就是从这里获得的 status 参数
    @ApiOperation("启用禁用员工账号")
    public Result startOrStop(@PathVariable Integer status, Long id){
        // status 是路径参数 ————> 添加路径注解 @PathVariable
        // id 是通过地址栏传参获得的 ————> 仅保证参数名一直即可
        log.info("启用禁用员工账号: {}, {}", status, id);

        // 调用 Service 真正的实现业务功能
        employeeService.startOrStop(status, id);

        return Result.success();
    }

    /**
     * 根据ID查询员工信息
     * @param id
     * @return
     */
    @GetMapping("{id}")
    @ApiOperation("根据ID查询员工信息")
    public Result<Employee> getByID(@PathVariable Long id){
        // 调用 Service 真正的实现业务功能
        Employee employee = employeeService.getByID(id);
        // 期望返回的是一个 Employee 对象

        return Result.success(employee);
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping // 不用添加路径, 已经在类上添加过了
    @ApiOperation("编辑员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        // 注意, 这里还是采用 EmployeeDTO 来接收前端传过来的数据
        //       数据类型被定义为 json, 因此需要添加: @RequestBody

        log.info("编辑员工信息");

        // 调用 Service 真正的实现业务功能
        employeeService.update(employeeDTO);

        return Result.success();
    }
}
