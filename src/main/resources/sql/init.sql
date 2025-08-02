-- 创建数据库
CREATE DATABASE IF NOT EXISTS excel_export DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE excel_export;

-- 创建用户表（示例数据表）
CREATE TABLE IF NOT EXISTS `user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `age` int(3) DEFAULT NULL COMMENT '年龄',
    `gender` varchar(10) DEFAULT NULL COMMENT '性别',
    `department` varchar(100) DEFAULT NULL COMMENT '部门',
    `position` varchar(100) DEFAULT NULL COMMENT '职位',
    `salary` decimal(10,2) DEFAULT NULL COMMENT '薪资',
    `join_time` datetime DEFAULT NULL COMMENT '入职时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_department` (`department`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建导出任务表
CREATE TABLE IF NOT EXISTS `export_task` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` varchar(64) NOT NULL COMMENT '任务唯一标识',
    `task_name` varchar(200) NOT NULL COMMENT '任务名称',
    `export_type` varchar(50) NOT NULL COMMENT '导出类型',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING,PROCESSING,SUCCESS,FAILED',
    `total_count` bigint(20) DEFAULT 0 COMMENT '总记录数',
    `processed_count` bigint(20) DEFAULT 0 COMMENT '已处理记录数',
    `progress` decimal(5,2) DEFAULT 0.00 COMMENT '进度百分比',
    `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
    `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
    `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
    `error_message` text DEFAULT NULL COMMENT '错误信息',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务表';

-- 插入测试用户数据（生成100万条测试数据的存储过程）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS GenerateTestUsers(IN record_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE current_batch INT DEFAULT 0;
    
    -- 清空现有数据
    TRUNCATE TABLE `user`;
    
    -- 关闭自动提交
    SET autocommit = 0;
    
    WHILE i <= record_count DO
        INSERT INTO `user` (
            `username`, `real_name`, `email`, `phone`, `age`, `gender`, 
            `department`, `position`, `salary`, `join_time`, `create_time`
        ) VALUES (
            CONCAT('user', LPAD(i, 8, '0')),
            CONCAT('用户', i),
            CONCAT('user', i, '@example.com'),
            CONCAT('1', LPAD(FLOOR(RAND() * 9999999999), 10, '0')),
            FLOOR(RAND() * 40) + 20,
            CASE WHEN RAND() > 0.5 THEN '男' ELSE '女' END,
            CASE 
                WHEN RAND() < 0.2 THEN '技术部'
                WHEN RAND() < 0.4 THEN '产品部'
                WHEN RAND() < 0.6 THEN '运营部'
                WHEN RAND() < 0.8 THEN '市场部'
                ELSE '人事部'
            END,
            CASE 
                WHEN RAND() < 0.1 THEN '总监'
                WHEN RAND() < 0.3 THEN '经理'
                WHEN RAND() < 0.6 THEN '主管'
                ELSE '专员'
            END,
            ROUND(RAND() * 20000 + 5000, 2),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 1000) DAY),
            NOW()
        );
        
        SET i = i + 1;
        SET current_batch = current_batch + 1;
        
        -- 每批次提交一次
        IF current_batch >= batch_size THEN
            COMMIT;
            SET current_batch = 0;
            -- 输出进度
            SELECT CONCAT('已生成 ', i - 1, ' 条记录') AS progress;
        END IF;
    END WHILE;
    
    -- 提交剩余数据
    COMMIT;
    
    -- 恢复自动提交
    SET autocommit = 1;
    
    SELECT CONCAT('成功生成 ', record_count, ' 条测试用户数据') AS result;
END //
DELIMITER ;

-- 生成10万条测试数据（可根据需要调整数量）
-- CALL GenerateTestUsers(100000);

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_user_department_create_time ON `user`(`department`, `create_time`);
CREATE INDEX IF NOT EXISTS idx_user_join_time ON `user`(`join_time`);
CREATE INDEX IF NOT EXISTS idx_export_task_status_create_time ON `export_task`(`status`, `create_time`);

-- 插入一些基础测试数据
INSERT IGNORE INTO `user` (
    `username`, `real_name`, `email`, `phone`, `age`, `gender`, 
    `department`, `position`, `salary`, `join_time`
) VALUES 
('admin', '管理员', 'admin@example.com', '13800138000', 30, '男', '技术部', '总监', 25000.00, '2020-01-01 09:00:00'),
('test001', '测试用户1', 'test001@example.com', '13800138001', 25, '女', '产品部', '经理', 15000.00, '2021-06-01 09:00:00'),
('test002', '测试用户2', 'test002@example.com', '13800138002', 28, '男', '技术部', '主管', 12000.00, '2022-03-01 09:00:00'),
('test003', '测试用户3', 'test003@example.com', '13800138003', 26, '女', '运营部', '专员', 8000.00, '2022-08-01 09:00:00'),
('test004', '测试用户4', 'test004@example.com', '13800138004', 32, '男', '市场部', '经理', 18000.00, '2021-12-01 09:00:00');