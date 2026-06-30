package com.personblog.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatEventVO {

    public static final ChatEventVO DONE_EVENT = ChatEventVO.builder()
            .type("done")
            .build();

    private Object data;

    private String type;
}
