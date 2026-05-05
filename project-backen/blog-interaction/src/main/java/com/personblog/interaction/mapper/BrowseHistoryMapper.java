package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.BrowseHistory;
import com.personblog.interaction.vo.BrowseHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 浏览历史表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    void batchInsertOrUpdate(@Param("list") List<BrowseHistory> list);

    List<BrowseHistoryVO> selectUserBrowseHistory(@Param("userId") Long userId, @Param("offset") long offset, @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);
}
