package com.example.excel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.excel.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 分页查询用户数据（用于导出）
     * 使用流式查询，避免一次性加载大量数据到内存
     *
     * @param page 分页参数
     * @param params 查询条件
     * @return 用户分页数据
     */
    IPage<User> selectUserPageForExport(Page<User> page, @Param("params") Map<String, Object> params);

    /**
     * 统计符合条件的用户总数
     *
     * @param params 查询条件
     * @return 总数
     */
    Long countUserForExport(@Param("params") Map<String, Object> params);

    /**
     * 流式查询用户数据（用于大数据量导出）
     * 使用游标方式，逐条读取数据，避免内存溢出
     *
     * @param params 查询条件
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户列表
     */
    List<User> selectUserListForExport(@Param("params") Map<String, Object> params,
                                       @Param("offset") Long offset,
                                       @Param("limit") Integer limit);
}