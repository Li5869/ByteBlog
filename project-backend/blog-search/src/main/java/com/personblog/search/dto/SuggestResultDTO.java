package com.personblog.search.dto;

import com.personblog.search.vo.SuggestItemVO;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class SuggestResultDTO {
    private List<SuggestItemVO> articles = Collections.emptyList();
    private List<SuggestItemVO> authors = Collections.emptyList();
    private List<SuggestItemVO> columns = Collections.emptyList();
}