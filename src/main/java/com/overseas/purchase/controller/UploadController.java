//上传头像接口
package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    // 使用项目根目录下的 uploads 目录，与 WebMvcConfig 保持一致
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "avatar");
    }

    @PostMapping("/product")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "product");
    }

    private Result<Map<String, String>> uploadFile(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            // 获取原文件名
            String originalFilename = file.getOriginalFilename();
            // 获取文件后缀
            String suffix = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 生成新文件名：时间戳 + UUID + 后缀
            String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + 
                              UUID.randomUUID().toString().substring(0, 6) + suffix;

            // 创建目标文件
            File dest = new File(UPLOAD_DIR + subDir + File.separator + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            // 保存文件
            file.transferTo(dest);
            
            log.info(subDir + "图片已保存至: {}", dest.getAbsolutePath());

            // 返回文件访问路径
            String fileUrl = "/api/upload/" + subDir + "/" + fileName;
            
            Map<String, String> map = new HashMap<>();
            map.put("url", fileUrl);
            
            return Result.success(map);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/avatar/{filename:.+}")
    public void getAvatar(@PathVariable String filename, HttpServletResponse response) {
        if (filename.contains("..")) {
            response.setStatus(403);
            return;
        }
        File file = new File(UPLOAD_DIR + "avatar/" + filename);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            
            // 设置内容类型
            String suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if ("jpg".equals(suffix) || "jpeg".equals(suffix)) {
                response.setContentType("image/jpeg");
            } else if ("png".equals(suffix)) {
                response.setContentType("image/png");
            } else if ("gif".equals(suffix)) {
                response.setContentType("image/gif");
            } else {
                response.setContentType("application/octet-stream");
            }
            
            byte[] buffer = new byte[1024];
            int b;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取头像失败", e);
            response.setStatus(500);
        }
    }
}