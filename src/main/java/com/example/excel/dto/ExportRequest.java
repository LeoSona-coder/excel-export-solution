package com.example.excel.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 导出请求DTO
 */
@Data
public class ExportRequest {

    /**
     * 导出类型
     */
    private String exportType;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 查询条件
     */
    private Map<String, Object> queryParams;

    /**
     * 是否异步导出（默认true）
     */
    private Boolean async = true;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 导出字段（为空则导出全部字段）
     */
    private String[] fields;

    /**
     * 文件名前缀
     */
    private String fileNamePrefix;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 部门
     */
    private String department;
}