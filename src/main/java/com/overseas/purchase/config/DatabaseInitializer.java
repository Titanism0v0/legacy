package com.overseas.purchase.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 用于在应用启动时修复数据库结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking database schema...");
        
        // 尝试添加 currency 字段
        try {
            // 先检查字段是否存在，如果不存在则添加
            // MySQL 8.0 以下不支持 IF NOT EXISTS 语法用于 ADD COLUMN，所以直接捕获异常
            String sql = "ALTER TABLE product ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '货币单位'";
            jdbcTemplate.execute(sql);
            log.info("Successfully added 'currency' column to 'product' table.");
        } catch (Exception e) {
            // 如果是因为字段已存在导致的错误，则忽略
            if (e.getMessage().contains("Duplicate column name")) {
                log.info("'currency' column already exists in 'product' table.");
            } else {
                log.warn("Failed to add 'currency' column: {}", e.getMessage());
            }
        }
        
        log.info("Database schema check completed.");
    }
}
