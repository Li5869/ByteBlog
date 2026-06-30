package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.dto.FollowDTO;
import com.personblog.interaction.entity.Follow;
import com.personblog.interaction.vo.FollowVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 关注表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
public interface FollowService extends IService<Follow> {

    FollowVO doFollow(FollowDTO dto);

    List<Long> checkBatchFollowStatus(List<Long> followingIds);

    List<Long> getFollowingIds(Long userId);
}
