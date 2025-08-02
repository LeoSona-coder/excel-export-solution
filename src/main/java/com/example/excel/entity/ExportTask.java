package com.example.excel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出任务实体类
 * 用于记录和管理导出任务的状态
 */
@Data
@TableName("export_task")
public class ExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一标识
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 导出类型
     */
    private String exportType;

    /**
     * 任务状态：PENDING(待处理), PROCESSING(处理中), SUCCESS(成功), FAILED(失败)
     */
    private String status;

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 已处理记录数
     */
    private Long processedCount;

    /**
     * 进度百分比
     */
    private Double progress;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人
     */
    private String createBy;

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
     * 更新时间
     */
    private LocalDateTime updateTime;
}