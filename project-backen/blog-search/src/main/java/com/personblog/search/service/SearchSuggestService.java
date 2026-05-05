package com.personblog.search.service;

import com.personblog.search.dto.SuggestResultDTO;

public interface SearchSuggestService {

    /**
     * 搜索建议（Completion Suggester）
     *
     * @param keyword 用户输入的前缀关键词
     * @param size    每类最多返回条数
     * @return 建议结果
     */
    SuggestResultDTO suggest(String keyword, int size);
}