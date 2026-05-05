package com.personblog.common.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.common.dto.AdminTagQueryDTO;
import com.personblog.common.entity.Tag;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.mapper.TagMapper;
import com.personblog.common.service.ITagService;
import com.personblog.common.vo.AdminTagVO;
import com.personblog.common.vo.TagVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

    // 本地缓存实例
    private Cache<String, List<TagVO>> tagCache;

    @PostConstruct
    public void initCache() {
        tagCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    /**
     * 获取标签列表
     * 缓存策略：Caffeine 本地缓存，30分钟过期
     *
     * @param size 限制返回数量，null或0返回全部
     * @return 标签列表（按使用次数降序）
     */
    @Override
    public List<TagVO> getTagList(Integer size) {
        int limit = (size == null || size <= 0) ? 100 : Math.min(size, 100);
        String cacheKey = "list:" + limit;

        // 先从缓存获取
        List<TagVO> cached = tagCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("标签缓存命中: size={}", limit);
            return cached;
        }

        // 查询数据库
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getUseCount).last("LIMIT " + limit);
        List<TagVO> result = list(wrapper).stream()
                .map(tag -> BeanUtil.copyProperties(tag, TagVO.class))
                .collect(Collectors.toList());

        // 写入缓存
        tagCache.put(cacheKey, result);
        log.debug("标签数据已缓存: size={}, count={}", limit, result.size());

        return result;
    }

    /**
     * 创建标签 - 清除缓存
     */
    public boolean saveTag(Tag tag) {
        boolean result = super.save(tag);
        if (result) {
            tagCache.invalidateAll();
            log.info("创建标签后清除缓存");
        }
        return result;
    }

    /**
     * 批量更新标签使用次数 - 清除缓存
     */
    public void invalidateTagCache() {
        tagCache.invalidateAll();
        log.info("标签使用次数更新后清除缓存");
    }

    @Override
    public List<TagVO> getTagListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Tag::getId, ids);
        return list(wrapper).stream()
                .map(tag -> BeanUtil.copyProperties(tag, TagVO.class))
                .collect(Collectors.toList());
    }

    // ==================== 管理端接口实现 ====================

    @Override
    public Page<AdminTagVO> getAdminTagList(AdminTagQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(Tag::getName, dto.getKeyword());
        }

        // 排序
        if ("use_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Tag::getUseCount);
        } else {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Tag::getCreatedAt);
        }

        Page<Tag> page = new Page<>(current, size);
        this.page(page, wrapper);

        // 转换为 VO
        Page<AdminTagVO> voPage = new Page<>(current, size, page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(tag -> AdminTagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .usageCount(tag.getUseCount() != null ? tag.getUseCount() : 0L)
                        .createdAt(tag.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminTagVO createTag(Tag tag) {
        // 检查标签名称是否重复
        boolean exists = this.lambdaQuery()
                .eq(Tag::getName, tag.getName())
                .exists();
        if (exists) {
            throw new BizException(BizCodeEnum.TAG_REPEAT.getCode(), "标签名称已存在");
        }

        tag.setUseCount(0L);
        tag.setCreatedAt(LocalDateTime.now());
        this.save(tag);

        tagCache.invalidateAll();

        return AdminTagVO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .usageCount(0L)
                .createdAt(tag.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTagByAdmin(Tag tag) {
        Tag existing = this.getById(tag.getId());
        if (existing == null) {
            throw new BizException(BizCodeEnum.TAG_NOT_EXIST);
        }

        // 如果修改了名称，检查是否重复
        if (!existing.getName().equals(tag.getName())) {
            boolean exists = this.lambdaQuery()
                    .eq(Tag::getName, tag.getName())
                    .ne(Tag::getId, tag.getId())
                    .exists();
            if (exists) {
                throw new BizException(BizCodeEnum.TAG_REPEAT.getCode(), "标签名称已存在");
            }
        }

        this.updateById(tag);
        tagCache.invalidateAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagByAdmin(Long id) {
        Tag tag = this.getById(id);
        if (tag == null) {
            throw new BizException(BizCodeEnum.TAG_NOT_EXIST);
        }

        this.removeById(id);
        tagCache.invalidateAll();
    }
}
