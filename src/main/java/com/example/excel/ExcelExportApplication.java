package com.example.excel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Excel导出应用主启动类
 * 启用异步处理和MyBatis扫描
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.example.excel.mapper")
public class ExcelExportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelExportApplication.class, args);
    }
}