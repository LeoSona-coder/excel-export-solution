package com.example.excel.service;

import com.example.excel.entity.ExportTask;
import com.example.excel.mapper.ExportTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件下载服务
 */
@Slf4j
@Service
public class FileDownloadService {

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    /**
     * 下载导出文件
     *
     * @param taskId 任务ID
     * @return 文件响应
     */
    public ResponseEntity<Resource> downloadFile(String taskId) {
        // 查询任务信息
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (!"SUCCESS".equals(task.getStatus())) {
            throw new RuntimeException("任务未完成或已失败");
        }

        if (!StringUtils.hasText(task.getFilePath())) {
            throw new RuntimeException("文件路径不存在");
        }

        // 检查文件是否存在
        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }

        try {
            // 创建文件资源
            Resource resource = new FileSystemResource(file);
            
            // 编码文件名，支持中文
            String encodedFileName = URLEncoder.encode(task.getFileName(), 
                StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            
            // 构建响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename*=UTF-8''" + encodedFileName);
            headers.add(HttpHeaders.CONTENT_TYPE, 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            
            log.info("开始下载文件: {}, 大小: {} bytes", task.getFileName(), file.length());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (UnsupportedEncodingException e) {
            log.error("文件名编码失败: {}", task.getFileName(), e);
            throw new RuntimeException("文件下载失败");
        }
    }

    /**
     * 检查文件是否可下载
     *
     * @param taskId 任务ID
     * @return 是否可下载
     */
    public boolean isFileDownloadable(String taskId) {
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null || !"SUCCESS".equals(task.getStatus())) {
            return false;
        }
        
        if (!StringUtils.hasText(task.getFilePath())) {
            return false;
        }
        
        File file = new File(task.getFilePath());
        return file.exists() && file.canRead();
    }

    /**
     * 获取文件信息
     *
     * @param taskId 任务ID
     * @return 文件信息
     */
    public FileInfo getFileInfo(String taskId) {
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return null;
        }
        
        FileInfo fileInfo = new FileInfo();
        fileInfo.setTaskId(taskId);
        fileInfo.setFileName(task.getFileName());
        fileInfo.setFileSize(task.getFileSize());
        fileInfo.setStatus(task.getStatus());
        fileInfo.setDownloadable(isFileDownloadable(taskId));
        
        return fileInfo;
    }

    /**
     * 文件信息内部类
     */
    public static class FileInfo {
        private String taskId;
        private String fileName;
        private Long fileSize;
        private String status;
        private boolean downloadable;

        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isDownloadable() {
            return downloadable;
        }

        public void setDownloadable(boolean downloadable) {
            this.downloadable = downloadable;
        }
    }
}