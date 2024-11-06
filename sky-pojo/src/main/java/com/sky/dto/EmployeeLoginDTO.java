package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "员工登录时传递的数据模型") // 对于实体的描述
public class EmployeeLoginDTO implements Serializable {

    @ApiModelProperty("用户名") // 对于属性的描述
    private String username;

    @ApiModelProperty("密码")
    private String password;

}
