package com.personblog.article.controller.Column;

import com.personblog.article.dto.ColumnArticleDTO;
import com.personblog.article.service.IColumnService;
import com.personblog.article.vo.MyArticleVO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专栏文章控制器
 * 负责专栏文章的管理
 *
 * @author LSH
 */
@RestController
@RequestMapping("/article/columns")
@RequiredArgsConstructor
@Tag(name = "专栏文章管理", description = "专栏文章的管理接口，包括添加、移除、查询可添加的文章")
public class ColumnArticleController {

    private final IColumnService columnService;

    /**
     * 添加文章到专栏
     * 仅作者可操作，文章必须已发布且属于当前用户
     * 同一文章不能重复添加到同一专栏
     * 专栏文章数量上限：100篇
     *
     * @param id  专栏ID
     * @param dto 文章ID列表
     * @return 成功响应
     */
    @Operation(summary = "添加文章到专栏", description = "添加文章到专栏，仅作者可操作")
    @PostMapping("/{id}/articles")
    public JsonData<Void> addArticles(
            @Parameter(description = "专栏ID") @PathVariable Long id,
            @Valid @RequestBody ColumnArticleDTO dto) {
        Long userId = UserContextHolder.getUserId();
        columnService.addArticles(userId, id, dto);
        return JsonData.buildSuccess();
    }

    /**
     * 从专栏移除文章
     * 仅作者可操作，文章本身不受影响
     *
     * @param id  专栏ID
     * @param dto 文章ID列表
     * @return 成功响应
     */
    @Operation(summary = "从专栏移除文章", description = "从专栏移除文章，仅作者可操作")
    @DeleteMapping("/{id}/articles")
    public JsonData<Void> removeArticles(
            @Parameter(description = "专栏ID") @PathVariable Long id,
            @Valid @RequestBody ColumnArticleDTO dto) {
        Long userId = UserContextHolder.getUserId();
        columnService.removeArticles(userId, id, dto);
        return JsonData.buildSuccess();
    }

    /**
     * 获取可添加到专栏的文章
     * 返回用户已发布但不在该专栏中的文章
     * 最多返回100篇，按创建时间降序排列
     *
     * @param id 专栏ID
     * @return 可添加的文章列表
     */
    @Operation(summary = "获取可添加的文章", description = "获取可添加到专栏的文章列表")
    @GetMapping("/{id}/available-articles")
    public JsonData<List<MyArticleVO>> getAvailableArticles(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        List<MyArticleVO> list = columnService.getAvailableArticles(userId, id);
        return JsonData.buildSuccess(list);
    }
}
