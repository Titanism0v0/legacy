package com.overseas.purchase.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadControllerSecurityTest {

    private MockMvc mockMvc;

    @TempDir
    private Path uploadDir;

    @BeforeEach
    void setUp() {
        UploadController uploadController = new UploadController();
        ReflectionTestUtils.setField(uploadController, "uploadDir", uploadDir.toString());
        mockMvc = MockMvcBuilders.standaloneSetup(uploadController).build();
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/upload/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("上传文件不能为空"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void oversizedFileIsRejectedBeforeWritingToDisk() throws Exception {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        content[0] = (byte) 0x89;
        content[1] = 0x50;
        content[2] = 0x4E;
        content[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", content);

        mockMvc.perform(multipart("/upload/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("上传文件大小不能超过 5MB"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void htmlFilesAreRejectedBeforeWritingToDisk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "xss.html", "text/html", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload/community").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("不支持的文件类型"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void javascriptFilesAreRejectedBeforeWritingToDisk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.js", "application/javascript", "alert(1)".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload/community").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("不支持的文件类型"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void svgFilesAreRejectedBeforeWritingToDisk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.svg", "image/svg+xml", "<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload/community").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("不支持的文件类型"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void pngExtensionWithHtmlBodyIsRejectedBeforeWritingToDisk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/upload/community").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("文件内容与图片类型不匹配"));

        assertUploadDirectoryEmpty();
    }

    @Test
    void validPngIsSavedWithGeneratedPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "safe.png", "image/png", pngBytes());

        mockMvc.perform(multipart("/upload/product").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url", startsWith("/api/upload/product/")));

        Path productDir = uploadDir.resolve("product");
        assertThat(Files.exists(productDir)).isTrue();
        assertThat(Files.list(productDir)).hasSize(1);
    }

    private void assertUploadDirectoryEmpty() throws Exception {
        if (!Files.exists(uploadDir)) {
            return;
        }
        assertThat(Files.walk(uploadDir).filter(Files::isRegularFile)).isEmpty();
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52
        };
    }
}
