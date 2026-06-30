package com.personblog.search.service;

import com.personblog.search.dto.SearchQueryDTO;
import com.personblog.search.dto.SearchResultDTO;

public interface SearchService {
    /**
     * 搜索
     *
     * @param queryDTO 搜索查询参数
     * @return 搜索结果
     */
    SearchResultDTO search(SearchQueryDTO queryDTO);
}
