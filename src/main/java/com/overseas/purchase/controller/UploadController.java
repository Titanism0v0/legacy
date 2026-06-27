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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_UPLOAD_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
            new java.util.HashSet<String>(java.util.Arrays.asList("jpg", "jpeg", "png", "gif"))
    );
    private static final Map<String, String> ALLOWED_MIME_TYPES = new HashMap<String, String>() {{
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("gif", "image/gif");
    }};

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

    @PostMapping("/community")
    public Result<Map<String, String>> uploadCommunityFile(@RequestParam("file") MultipartFile file) {
        return uploadFile(file, "community");
    }

    private Result<Map<String, String>> uploadFile(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = extractExtension(originalFilename);
            String validationError = validateUploadFile(file, extension);
            if (validationError != null) {
                return Result.error(validationError);
            }
            String suffix = "." + extension;

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
            return com.overseas.purchase.common.PublicErrorResponse.from("文件上传失败，请稍后重试", e);
        }
    }

    private String validateUploadFile(MultipartFile file, String extension) throws IOException {
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            return "上传文件大小不能超过 5MB";
        }
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            return "不支持的文件类型";
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_MIME_TYPES.get(extension).equals(contentType)) {
            return "不支持的文件类型";
        }
        if (!hasValidImageSignature(file, extension)) {
            return "文件内容与图片类型不匹配";
        }
        return null;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separatorIndex = contentType.indexOf(';');
        String normalized = separatorIndex >= 0 ? contentType.substring(0, separatorIndex) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasValidImageSignature(MultipartFile file, String extension) throws IOException {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = file.getInputStream()) {
            length = inputStream.read(header);
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
        }
        if ("png".equals(extension)) {
            return length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
        }
        if ("gif".equals(extension)) {
            return length >= 6
                    && header[0] == 0x47
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x38
                    && (header[4] == 0x37 || header[4] == 0x39)
                    && header[5] == 0x61;
        }
        return false;
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
