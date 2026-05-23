package com.personblog.common.dto.MqMessage.AIModerate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModerateMessage {
    //业务类型
    private String bizType;
    //内容
    private String content;
    //业务id
    private Long bizId;
    //作者id
    private Long authorId;
    //标题
    private String title;
}
