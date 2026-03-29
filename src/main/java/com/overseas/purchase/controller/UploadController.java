package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${file.upload.path:./uploads/}")
    private String uploadDir;

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "avatar");
    }

    @PostMapping("/product")
    public Result<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "product");
    }

    @PostMapping("/kyc")
    public Result<Map<String, String>> uploadKycFile(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "kyc");
    }

    @PostMapping("/payment-proof")
    public Result<Map<String, String>> uploadPaymentProof(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "payment-proof");
    }

    private Result<Map<String, String>> uploadFile(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }

            String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    + UUID.randomUUID().toString().substring(0, 6) + suffix;

            File dest = resolveUploadFile(subDir, fileName);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                return Result.error("创建上传目录失败");
            }

            file.transferTo(dest);
            log.info("{} 图片已保存至: {}", subDir, dest.getAbsolutePath());

            Map<String, String> map = new HashMap<>();
            map.put("url", "/api/upload/" + subDir + "/" + fileName);
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

        File file = resolveUploadFile("avatar", filename);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            String suffix = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
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
            int length;
            while ((length = fis.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            log.error("读取头像失败", e);
            response.setStatus(500);
        }
    }

    private File resolveUploadFile(String subDir, String fileName) {
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        return basePath.resolve(subDir).resolve(fileName).toFile();
    }
}
