package com.example.excel.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出响应DTO
 */
@Data
public class ExportResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 进度百分比
     */
    private Double progress;

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 已处理记录数
     */
    private Long processedCount;

    /**
     * 文件下载URL
     */
    private String downloadUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否完成
     */
    public boolean isCompleted() {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 是否处理中
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(status);
    }
}