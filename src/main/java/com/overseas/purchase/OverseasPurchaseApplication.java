package com.overseas.purchase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 海外代购系统启动类
 * 
 * @author System
 */
@SpringBootApplication
@MapperScan("com.overseas.purchase.mapper")
public class OverseasPurchaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(OverseasPurchaseApplication.class, args);
        System.out.println("=================================");
        System.out.println("海外代购系统启动成功！");
        System.out.println("=================================");
    }
}
