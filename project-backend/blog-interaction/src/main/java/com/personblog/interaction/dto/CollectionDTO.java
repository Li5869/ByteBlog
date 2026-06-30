package com.personblog.interaction.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class CollectionDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;

    private Long articleAuthorId;

    private Boolean isCollection;
}
