package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    List<Conversation> selectConversationsByUserId(@Param("userId") Long userId);

    @Update("UPDATE tb_conversation SET unread_count = 0 WHERE user_id = #{userId} AND target_user_id = #{targetUserId}")
    int resetUnreadCount(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
}
