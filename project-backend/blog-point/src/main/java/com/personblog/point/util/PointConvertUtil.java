package com.personblog.point.util;

import com.personblog.point.entity.PointLog;
import com.personblog.point.vo.PointLogVO;

public class PointConvertUtil {

    private PointConvertUtil() {
    }

    /**
     * PointLog 实体转 PointLogVO
     */
    public static PointLogVO toPointLogVO(PointLog pointLog) {
        return PointLogVO.builder()
                .id(pointLog.getId())
                .points(pointLog.getPoints())
                .type(pointLog.getType())
                .description(pointLog.getDescription())
                .bizId(pointLog.getBizId())
                .createdAt(pointLog.getCreatedAt())
                .build();
    }
}
