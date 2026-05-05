package com.personblog.api.searchAPI;

import com.personblog.common.dto.Search.QuestionSearchDTO;

import java.util.List;

/**
 * 问题搜索数据API —— 供blog-search模块从blog-question拉取问题数据
 */
public interface QuestionSearchDataApi {

    /**
     * 获取所有问题的搜索数据
     */
    List<QuestionSearchDTO> listAllQuestionsForSearch();

    /**
     * 获取单个问题的搜索数据
     */
    QuestionSearchDTO getQuestionForSearch(Long questionId);
}
