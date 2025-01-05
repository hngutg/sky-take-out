package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    // 微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxlogin(UserLoginDTO userLoginDTO) {

        // 和传统的用户名密码登录是不同的
        // 1. 调用微信服务器接口, 获得当前微信用户的 openId
        String openid = getOpenId(userLoginDTO.getCode());

        // 2. 判断openId是否为空, 为空, 则登录失败
        if (openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 3. 判断针对于自己这个系统, 当前用户是否为新用户
        //    当前用户的openId是否在自己的用户表中 ————> 如果是新的, 需要存储(自动完成注册)
        User user = userMapper.getByOpenId(openid);
        if (user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            // 注意: 之前写的AOP的切点是mapper包下的所有文件，在service层用不了 ————> 注意AOP所设定的范围
            // 其他信息还获取不到, 没关系 ————> 先把User对象创建起来即可

            // 将当前用户插入到本地表中
            userMapper.insert(user);
            // 如果这个用户是新用户，就需要先插入数据后获取主键，所以需要主键返回  (返回的主键值被放入到了user中)
        }

        // 4. 返回当前登录的用户
        return user;
    }

    /**
     * 调用微信接口服务, 获取微信用户的openID
     * @param code 登录时获取的 code，可通过wx.login获取
     * @return
     */
    private String getOpenId(String code) {
        // map即为要发送的信息, 而具体信息有什么, 一共有四个, 可以看postMan
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        // 将所有的信息封装到map中进行发送
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        // HttpClientUtil: Http工具类, 利用这个工具类来发送请求, 其中第二个参数即为要发送的信息 (payload)

        // 利用 fastjson.JSON 对 json 进行解析, 把里面的 openId 拿出来
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
