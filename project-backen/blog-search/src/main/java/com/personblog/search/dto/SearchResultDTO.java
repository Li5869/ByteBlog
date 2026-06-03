package com.personblog.search.dto;


import com.personblog.search.vo.ArticleSearchVO;
import com.personblog.search.vo.AuthorSearchVO;
import com.personblog.search.vo.ColumnSearchVO;
import lombok.Data;

import java.util.List;

@Data
public class SearchResultDTO {

    private List<ArticleSearchVO> articles;

    private List<AuthorSearchVO> authors;

    private List<ColumnSearchVO> columns;

    private Long articleTotal;

    private Long authorTotal;

    private Long columnTotal;
}