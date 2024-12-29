package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@Slf4j
@Api(tags = "通用接口")
@RestController
@RequestMapping("/admin/common")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @ApiOperation("文件上传")
    @PostMapping("/upload") // 请求方式为post
    public Result<String> upload(MultipartFile file){
        // 这里传入的参数为一个文件, 因此采用 MultipartFile 进行接收(利用SpringMVC框架), 注意参数名应当与前端提交的参数名一致
        log.info("文件上传: {}", file);

        // 将文件上传到阿里云去
        // 在实现了对于 AliOssUtil 对象的创建后 (配置类中创建并交给IOC容器), 在这里就可以直接注入了
        try {
            // 原始文件名:
            String originalFilename = file.getOriginalFilename();
            // 截取后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 这里的文件名, 采用了UUID来生成名称(防止重名)
            String objectName = UUID.randomUUID().toString() + extension;

            String filePath = aliOssUtil.upload(file.getBytes(), objectName); // 返回文件的请求路径
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
