package com.personblog.job.handler;

import com.personblog.api.interactionAPI.BrowseHistoryApi;
import com.personblog.api.interactionAPI.LikeApi;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;

@Component
@Slf4j
@RequiredArgsConstructor
public class InteractionJobHandler {
    private final LikeApi likeApi;
    private final BrowseHistoryApi browseHistoryApi;
    private final
    List<String> types = List.of(ARTICLE, COMMENT);

    @XxlJob("handleLikesTime")
    public void handleLikesTime() {
        log.info("开始处理点赞");
        for (String type : types) {
            likeApi.readLikesTimesAnd2DB(type, 30);
        }
    }

    @XxlJob("browseHistorySyncJob")
    public void handleBrowseHistory() {
        log.info("开始同步浏览历史到数据库");
        browseHistoryApi.syncBrowseHistory2DB();
        log.info("浏览历史同步完成");
    }
}
