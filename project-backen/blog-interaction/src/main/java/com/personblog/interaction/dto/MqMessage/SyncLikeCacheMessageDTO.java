package com.personblog.interaction.dto.MqMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLikeCacheMessageDTO {
    private String targetType;
}
