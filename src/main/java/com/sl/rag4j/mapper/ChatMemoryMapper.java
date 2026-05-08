package com.sl.rag4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sl.rag4j.entity.ChatMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天记忆数据访问接口
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemory> {

    /**
     * 根据用户名和知识库ID查询聊天历史列表，按memoryId倒序排列
     */
    @Select("SELECT * FROM chat_memory WHERE user_name = #{userName} AND kb_id = #{kbId} ORDER BY memory_id DESC")
    List<ChatMemory> selectByUserAndKbId(@Param("userName") String userName, @Param("kbId") Long kbId);
}