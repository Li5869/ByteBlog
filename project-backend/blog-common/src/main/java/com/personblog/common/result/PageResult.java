package com.personblog.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> of(Long total, Long pages, Long current, Long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPages(pages);
        result.setCurrent(current);
        result.setSize(size);
        result.setRecords(records);
        return result;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setTotal(0L);
        result.setPages(0L);
        result.setCurrent(1L);
        result.setSize(10L);
        result.setRecords(Collections.emptyList());
        return result;
    }
}
