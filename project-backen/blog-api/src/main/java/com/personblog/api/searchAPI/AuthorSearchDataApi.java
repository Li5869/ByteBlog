package com.personblog.api.searchAPI;

import com.personblog.common.dto.Search.AuthorSearchDTO;

import java.util.List;

/**
 * 作者搜索数据API —— 供blog-search模块从blog-security拉取用户数据
 */
public interface AuthorSearchDataApi {

    /**
     * 获取所有正常状态作者的搜索数据
     */
    List<AuthorSearchDTO> listAllAuthorsForSearch();

    /**
     * 获取单个作者的搜索数据
     */
    AuthorSearchDTO getAuthorForSearch(Long authorId);
}
