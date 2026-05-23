package com.personblog.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.dto.MqMessage.user.UserLikeMessageDTO;
import com.personblog.security.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    void batchUpdateLikeCount(List<UserLikeMessageDTO> dtoList);
}
