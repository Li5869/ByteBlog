package com.personblog.interaction.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LikedVO {
    //点赞个数
    private Long likes;
}
