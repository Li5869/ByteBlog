package com.personblog.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallsDTO {
    //工具id
    private Long id;
    //工具名称
    private String name;
    //工具参数
    private String args;
    //工具结果
    private String result;
}
