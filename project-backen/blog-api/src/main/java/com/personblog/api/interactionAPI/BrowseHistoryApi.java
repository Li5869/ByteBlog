package com.personblog.api.interactionAPI;

public interface BrowseHistoryApi {

    void recordBrowse(Long userId, Long articleId);

    void syncBrowseHistory2DB();

}
