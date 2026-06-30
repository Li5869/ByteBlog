package com.personblog.api.searchAPI;

import com.personblog.common.dto.Search.ColumnSearchDTO;

import java.util.List;

/**
 * 专栏搜索数据API —— 供blog-search模块从blog-article拉取专栏数据
 *
 * @author LSH
 */
public interface ColumnSearchDataApi {

    /**
     * 获取所有已发布专栏的搜索数据
     */
    List<ColumnSearchDTO> listAllColumnsForSearch();

    /**
     * 获取单个专栏的搜索数据
     */
    ColumnSearchDTO getColumnForSearch(Long columnId);
}
