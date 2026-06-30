package com.personblog.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.searchAPI.AuthorSearchDataApi;
import com.personblog.common.dto.Search.AuthorSearchDTO;
import com.personblog.security.entity.User;
import com.personblog.security.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 作者搜索数据服务 —— 实现AuthorSearchDataApi，为blog-search提供全量用户数据
 */
@Service
@RequiredArgsConstructor
public class AuthorSearchDataServiceImpl implements AuthorSearchDataApi {

    private final UserMapper userMapper;

    @Override
    public List<AuthorSearchDTO> listAllAuthorsForSearch() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus,  1)
        );
        return users.stream().map(this::convertToAuthorSearchDTO).collect(Collectors.toList());
    }

    @Override
    public AuthorSearchDTO getAuthorForSearch(Long authorId) {
        User user = userMapper.selectById(authorId);
        if (user == null || user.getStatus() != 1) {
            return null;
        }
        return convertToAuthorSearchDTO(user);
    }

    private AuthorSearchDTO convertToAuthorSearchDTO(User user) {
        return AuthorSearchDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .articlesCount(user.getArticlesCount())
                .fansCount(user.getFansCount())
                .likesCount(user.getLikesCount())
                .status(user.getStatus().intValue())
                .build();
    }
}
