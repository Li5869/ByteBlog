package com.personblog.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.admin.dto.AdminTagQueryDTO;
import com.personblog.admin.entity.Tag;
import com.personblog.admin.mapper.TagMapper;
import com.personblog.admin.service.ITagService;
import com.personblog.admin.vo.AdminTagVO;
import com.personblog.api.adminAPI.TagApi;
import com.personblog.api.adminAPI.vo.TagVO;
import com.personblog.common.dto.Tag.TagDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签表 服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService, TagApi {

    private Cache<String, List<TagVO>> tagCache;

    @PostConstruct
    public void initCache() {
        tagCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
    }

    @Override
    public List<TagVO> getTagList(Integer size) {
        int limit = (size == null || size <= 0) ? 100 : Math.min(size, 100);
        String cacheKey = "list:" + limit;

        List<TagVO> cached = tagCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("标签缓存命中: size={}", limit);
            return cached;
        }

        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getUseCount).last("LIMIT " + limit);
        List<TagVO> result = list(wrapper).stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .useCount(tag.getUseCount())
                        .build())
                .collect(Collectors.toList());

        tagCache.put(cacheKey, result);
        log.debug("标签数据已缓存: size={}, count={}", limit, result.size());

        return result;
    }

    @Override
    public List<TagVO> getTagListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Tag::getId, ids);
        return list(wrapper).stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .useCount(tag.getUseCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void invalidateTagCache() {
        tagCache.invalidateAll();
        log.info("标签使用次数更新后清除缓存");
    }

    @Override
    public Page<AdminTagVO> getAdminTagList(AdminTagQueryDTO dto) {
        int current = (dto.getCurrent() == null || dto.getCurrent() <= 0) ? 1 : dto.getCurrent();
        int size = (dto.getSize() == null || dto.getSize() <= 0) ? 10 : Math.min(dto.getSize(), 50);

        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(Tag::getName, dto.getKeyword());
        }

        if ("use_count".equals(dto.getSortField())) {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Tag::getUseCount);
        } else {
            wrapper.orderBy(true, "asc".equalsIgnoreCase(dto.getSortOrder()), Tag::getCreatedAt);
        }

        Page<Tag> page = new Page<>(current, size);
        this.page(page, wrapper);

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

    // ==================== TagApi 接口实现 ====================
    @Override
    public List<TagVO> getTagsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return getTagListByIds(ids.stream().toList());
    }

    @Override
    public List<TagDTO> getTagsByNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Tag::getName, names);
        return list(wrapper).stream()
                .map(tag -> {
                    TagDTO dto = new TagDTO();
                    dto.setId(tag.getId());
                    dto.setName(tag.getName());
                    dto.setUseCount(tag.getUseCount());
                    dto.setCreatedAt(tag.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public long countExistingTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Tag::getId, tagIds);
        return count(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTags(List<TagDTO> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<Tag> entityList = tags.stream()
                .map(dto -> {
                    Tag tag = new Tag();
                    tag.setName(dto.getName());
                    tag.setUseCount(dto.getUseCount() != null ? dto.getUseCount() : 0L);
                    tag.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
                    return tag;
                })
                .collect(Collectors.toList());
        saveBatch(entityList);
        tagCache.invalidateAll();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateTagUseCount(Set<Long> tagIds, int delta) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            lambdaUpdate()
                    .eq(Tag::getId, tagId)
                    .setSql("use_count = use_count + {0}", delta)
                    .update();
        }
        tagCache.invalidateAll();
    }
}
