package com.personblog.common.dto.Moderate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModerateMessage {
    
    private String bizType;

    private String content;

    private Long bizId;

    private Long authorId;

    private String title;
}
