package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
    
    @Select("SELECT * FROM tb_ai_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<AiMessage> selectByConversationId(@Param("conversationId") Long conversationId);
}
