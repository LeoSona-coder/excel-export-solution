package com.example.excel.controller;

import com.example.excel.dto.ExportRequest;
import com.example.excel.dto.ExportResponse;
import com.example.excel.entity.ExportTask;
import com.example.excel.mapper.ExportTaskMapper;
import com.example.excel.service.ExcelExportService;
import com.example.excel.service.FileDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel导出控制器
 * 提供导出相关的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*") 
public class ExcelExportController {

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private FileDownloadService fileDownloadService;

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    /**
     * 启动导出任务
     *
     * @param request 导出请求
     * @return 统一响应
     */
    @PostMapping("/start")
    public ApiResponse<ExportResponse> startExport(@RequestBody ExportRequest request) {
        try {
            // 参数校验
            if (!StringUtils.hasText(request.getExportType())) {
                return ApiResponse.error("导出类型不能为空");
            }
            
            if (!StringUtils.hasText(request.getCreateBy())) {
                request.setCreateBy("system"); // 默认创建人
            }
            
            // 设置默认任务名称
            if (!StringUtils.hasText(request.getTaskName())) {
                request.setTaskName("用户数据导出");
            }
            
            log.info("开始导出任务，类型: {}, 创建人: {}", 
                request.getExportType(), request.getCreateBy());
            
            ExportResponse response = excelExportService.startExport(request);
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("启动导出任务失败", e);
            return ApiResponse.error("启动导出任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询导出任务状态
     *
     * @param taskId 任务ID
     * @return 统一响应
     */
    @GetMapping("/status/{taskId}")
    public ApiResponse<ExportResponse> getExportStatus(@PathVariable String taskId) {
        try {
            if (!StringUtils.hasText(taskId)) {
                return ApiResponse.error("任务ID不能为空");
            }
            
            ExportResponse response = excelExportService.getExportStatus(taskId);
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("查询导出任务状态失败, taskId: {}", taskId, e);
            return ApiResponse.error("查询任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 下载导出文件
     *
     * @param taskId 任务ID
     * @return 文件流
     */
    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String taskId) {
        try {
            if (!StringUtils.hasText(taskId)) {
                throw new RuntimeException("任务ID不能为空");
            }
            
            return fileDownloadService.downloadFile(taskId);
            
        } catch (Exception e) {
            log.error("下载文件失败, taskId: {}", taskId, e);
            throw new RuntimeException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的导出任务列表
     *
     * @param createBy 创建人
     * @param limit 限制数量
     * @return 统一响应
     */
    @GetMapping("/tasks")
    public ApiResponse<List<ExportTask>> getUserTasks(
            @RequestParam(defaultValue = "system") String createBy,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<ExportTask> tasks = exportTaskMapper.selectUserTasks(createBy, limit);
            return ApiResponse.success(tasks);
            
        } catch (Exception e) {
            log.error("查询用户任务列表失败, createBy: {}", createBy, e);
            return ApiResponse.error("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     *
     * @param taskId 任务ID
     * @return 统一响应
     */
    @GetMapping("/file-info/{taskId}")
    public ApiResponse<FileDownloadService.FileInfo> getFileInfo(@PathVariable String taskId) {
        try {
            if (!StringUtils.hasText(taskId)) {
                return ApiResponse.error("任务ID不能为空");
            }
            
            FileDownloadService.FileInfo fileInfo = fileDownloadService.getFileInfo(taskId);
            if (fileInfo == null) {
                return ApiResponse.error("文件信息不存在");
            }
            
            return ApiResponse.success(fileInfo);
            
        } catch (Exception e) {
            log.error("获取文件信息失败, taskId: {}", taskId, e);
            return ApiResponse.error("获取文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取导出统计信息
     *
     * @return 统一响应
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getExportStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 获取正在处理的任务数
            int processingCount = exportTaskMapper.countProcessingTasks();
            statistics.put("processingCount", processingCount);
            
            // 可以添加更多统计信息
            statistics.put("timestamp", System.currentTimeMillis());
            
            return ApiResponse.success(statistics);
            
        } catch (Exception e) {
            log.error("获取导出统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 统一响应类
     */
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private long timestamp;

        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("success");
            response.setData(data);
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(500);
            response.setMessage(message);
            return response;
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setCode(code);
            response.setMessage(message);
            return response;
        }

        // Getters and Setters
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}