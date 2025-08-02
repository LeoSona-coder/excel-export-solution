package com.example.excel.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 用于演示百万级数据导出
 */
@Data
@TableName("user")
@ContentRowHeight(20)
@HeadRowHeight(25)
@ColumnWidth(15)
public class User {

    @TableId(type = IdType.AUTO)
    @ExcelProperty(value = "用户ID", index = 0)
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty(value = "用户名", index = 1)
    @ColumnWidth(20)
    private String username;

    @ExcelProperty(value = "真实姓名", index = 2)
    @ColumnWidth(15)
    private String realName;

    @ExcelProperty(value = "邮箱", index = 3)
    @ColumnWidth(25)
    private String email;

    @ExcelProperty(value = "手机号", index = 4)
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty(value = "年龄", index = 5)
    @ColumnWidth(10)
    private Integer age;

    @ExcelProperty(value = "性别", index = 6)
    @ColumnWidth(10)
    private String gender;

    @ExcelProperty(value = "部门", index = 7)
    @ColumnWidth(20)
    private String department;

    @ExcelProperty(value = "职位", index = 8)
    @ColumnWidth(20)
    private String position;

    @ExcelProperty(value = "薪资", index = 9)
    @ColumnWidth(15)
    private Double salary;

    @ExcelProperty(value = "入职时间", index = 10)
    @ColumnWidth(20)
    private LocalDateTime joinTime;

    @ExcelProperty(value = "创建时间", index = 11)
    @ColumnWidth(20)
    private LocalDateTime createTime;

    @ExcelProperty(value = "更新时间", index = 12)
    @ColumnWidth(20)
    private LocalDateTime updateTime;
}