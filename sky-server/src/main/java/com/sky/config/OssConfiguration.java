package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类, 用于创建 AliOssUtil 对象   (com. sky. utils. AliOssUtil)
 */
@Configuration // 表示当前类为一个配置类
@Slf4j
public class OssConfiguration {

    @Bean // 这样, 在项目启动时, 就会调用到这个方法, 将AliOssUtil对象创建出来, 交给Spring容器管理
    @ConditionalOnMissingBean // 条件对象, 当没有这个Bean时, 再进行创建
    // 通过这个方法, 将Util对象创建出来
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        // AliOssProperties 配置属性类 加了 @Component, 即交给了IOC容器管理
        // 可以直接将 aliOssProperties 对象注入
        log.info("开始创建阿里云文件上传工具类对象: {}", aliOssProperties);

        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
