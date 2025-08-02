package com.example.excel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.excel.entity.ExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 导出任务数据访问层
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {

    /**
     * 根据任务ID查询任务
     *
     * @param taskId 任务ID
     * @return 导出任务
     */
    ExportTask selectByTaskId(@Param("taskId") String taskId);

    /**
     * 更新任务进度
     *
     * @param taskId 任务ID
     * @param processedCount 已处理数量
     * @param progress 进度百分比
     * @return 更新行数
     */
    int updateProgress(@Param("taskId") String taskId,
                       @Param("processedCount") Long processedCount,
                       @Param("progress") Double progress);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 状态
     * @param errorMessage 错误信息（可选）
     * @return 更新行数
     */
    int updateStatus(@Param("taskId") String taskId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    /**
     * 更新文件信息
     *
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @return 更新行数
     */
    int updateFileInfo(@Param("taskId") String taskId,
                       @Param("filePath") String filePath,
                       @Param("fileName") String fileName,
                       @Param("fileSize") Long fileSize);

    /**
     * 查询正在处理的任务数量
     *
     * @return 处理中的任务数量
     */
    int countProcessingTasks();

    /**
     * 查询用户的导出任务列表
     *
     * @param createBy 创建人
     * @param limit 限制数量
     * @return 任务列表
     */
    List<ExportTask> selectUserTasks(@Param("createBy") String createBy,
                                     @Param("limit") Integer limit);
}