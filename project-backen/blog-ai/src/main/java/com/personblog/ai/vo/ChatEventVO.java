package com.personblog.ai.vo;

import com.personblog.ai.constants.ChatEventTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatEventVO {

    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();

    private Object eventData;

    private int eventType;
}
