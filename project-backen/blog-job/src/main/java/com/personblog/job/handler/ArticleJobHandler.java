package com.personblog.job.handler;

import com.personblog.api.articleAPI.ArticleAPI;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ArticleJobHandler {

    private final ArticleAPI articleAPI;

    /**
     * 刷新热门文章标记
     * 根据综合热度分（浏览量+点赞*10+评论*5+收藏*8）重新计算 Top N
     * */

    @XxlJob("refreshHotArticles")
    public void refreshHotArticles() {
        log.info("开始刷新热门文章");
        articleAPI.refreshHotArticles();
        log.info("热门文章刷新完成");
    }
}
