package com.personblog.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.notification.entity.BizNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BizNotificationMapper extends BaseMapper<BizNotification> {

    @Update("UPDATE tb_biz_notification SET is_read = true WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE tb_biz_notification SET is_read = true WHERE user_id = #{userId} AND is_read = false")
    int markAllAsRead(@Param("userId") Long userId);
}
