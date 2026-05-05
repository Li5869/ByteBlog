package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.BrowseHistory;
import com.personblog.interaction.vo.BrowseHistoryVO;

/**
 * <p>
 * 浏览历史表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface BrowseHistoryService extends IService<BrowseHistory> {


    Page<BrowseHistoryVO> getUserBrowseHistory(Long userId, Integer current, Integer size);
}
