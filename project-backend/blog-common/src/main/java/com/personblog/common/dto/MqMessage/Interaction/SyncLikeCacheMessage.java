package com.personblog.common.dto.MqMessage.Interaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLikeCacheMessage {
    private String targetType;

    private Long bizId;
}
