package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE tb_message SET is_read = true WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND is_read = false")
    int markAsReadBySender(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Update("UPDATE tb_message SET is_read = true WHERE receiver_id = #{receiverId} AND is_read = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);
}
