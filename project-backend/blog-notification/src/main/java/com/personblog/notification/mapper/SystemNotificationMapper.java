package com.personblog.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.notification.entity.SystemNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 系统通知表 Mapper 接口
 *
 * @author LSH
 * @since 2026-04-07
 */
@Mapper
public interface SystemNotificationMapper extends BaseMapper<SystemNotification> {

    @Update("UPDATE tb_system_notification SET is_read = true WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE tb_system_notification SET is_read = true WHERE user_id = #{userId} AND is_read = false")
    int markAllAsRead(@Param("userId") Long userId);
}
