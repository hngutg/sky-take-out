package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.json.JacksonObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
    }

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller")) // 指定要扫描的包 ———— 指定生成接口需要扫描的包
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    // 扩展消息转换器方式: 重写父类 WebMvcConfigurationSupport 中的一个方法即可  (方法是固定的)
    /**
     * 扩展 Spring MVC 框架的消息转换器 ————> 统一对后端返回给前端的数据进行处理
     * @param converters
     */
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters){
        // 这个方法在项目启动的时候就会执行
        log.info("扩展消息转换器...");

        // 创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 需要为消息转换器设置一个对象转换器
        // 对象转换器: 可以将java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper()); // 这里是用到的自定义的 JacksonObjectMapper 类

        // 在创建了消息转换器 converter 之后, 还并没有交给框架, 框架不会去使用这个转换器
        // 需要将其加入到消息转换器的容器 converters 中去 ————> converters 所存放的就是整个 Spring MVC 框架所使用的消息转换器(集合)
        
        // 之后, 将自己的消息转化器加入到容器中
        converters.add(0, converter);
        // 注意: converters 中是存在一些自带的消息转换器的。其中各个转换器之间是存在顺序的
        //       排在最后一个, 默认是使用不到的
        // 因此, 添加另一个参数 index, 修改自己所添加进去的转化器的顺序, 优先使用自己的消息转换器
    }
}
