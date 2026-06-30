package com.personblog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.article.entity.ColumnSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 专栏订阅Mapper接口
 *
 * @author LSH
 */
@Mapper
public interface ColumnSubscriptionMapper extends BaseMapper<ColumnSubscription> {

    /**
     * 检查用户是否已订阅专栏
     * @param userId 用户ID
     * @param columnId 专栏ID
     * @return 是否存在订阅记录
     */
    @Select("SELECT COUNT(1) > 0 FROM tb_column_subscription WHERE user_id = #{userId} AND column_id = #{columnId}")
    boolean exists(@Param("userId") Long userId, @Param("columnId") Long columnId);
}
