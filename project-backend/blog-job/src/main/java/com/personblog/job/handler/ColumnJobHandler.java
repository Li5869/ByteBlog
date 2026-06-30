package com.personblog.job.handler;

import com.personblog.api.columnAPI.ColumnApi;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ColumnJobHandler {
    private final ColumnApi columnApi;
    @XxlJob("columnViewHandler")
    public void handleColumnView2DB(){
        log.info("开始同步专栏浏览量到数据库");
        columnApi.updateColumnView();
        log.info("专栏浏览量同步完成");
    }
}
