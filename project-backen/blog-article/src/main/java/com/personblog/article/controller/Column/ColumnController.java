package com.personblog.article.controller.Column;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.article.dto.column.ColumnCreateDTO;
import com.personblog.article.dto.column.ColumnQueryDTO;
import com.personblog.article.dto.column.ColumnUpdateDTO;
import com.personblog.article.service.IColumnService;
import com.personblog.article.vo.ColumnCreateVO;
import com.personblog.article.vo.ColumnDetailVO;
import com.personblog.article.vo.ColumnListVO;
import com.personblog.article.vo.MyColumnVO;
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
 * 专栏控制器
 * 负责专栏的基础管理（CRUD）
 *
 * @author LSH
 */
@RestController
@RequestMapping("/article/columns")
@RequiredArgsConstructor
@Tag(name = "专栏管理", description = "专栏的基础管理接口，包括创建、更新、删除、查询等")
public class ColumnController {

    private final IColumnService columnService;

    /**
     * 创建专栏
     * 支持保存为草稿或直接发布
     *
     * @param dto 创建参数
     * @return 创建结果，包含专栏ID、标题、状态
     */
    @Operation(summary = "创建专栏", description = "创建新专栏，支持保存为草稿或直接发布")
    @PostMapping
    public JsonData<ColumnCreateVO> createColumn(@Valid @RequestBody ColumnCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        ColumnCreateVO vo = columnService.createColumn(userId, dto);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 更新专栏
     * 仅作者可操作，支持更新标题、描述、封面、状态
     *
     * @param id  专栏ID
     * @param dto 更新参数
     * @return 更新结果，包含专栏ID、标题、状态
     */
    @Operation(summary = "更新专栏", description = "更新专栏信息，仅作者可操作")
    @PutMapping("/{id}")
    public JsonData<ColumnCreateVO> updateColumn(
            @Parameter(description = "专栏ID") @PathVariable Long id,
            @Valid @RequestBody ColumnUpdateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        ColumnCreateVO vo = columnService.updateColumn(userId, id, dto);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 删除专栏
     * 仅作者可操作，逻辑删除
     * 删除时会同步删除专栏文章关联关系和订阅关系
     *
     * @param id 专栏ID
     * @return 成功响应
     */
    @Operation(summary = "删除专栏", description = "删除专栏，仅作者可操作，逻辑删除")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteColumn(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        columnService.deleteColumn(userId, id);
        return JsonData.buildSuccess();
    }

    /**
     * 获取专栏详情
     * 草稿状态仅作者可见
     * 返回专栏基本信息、作者信息、文章列表
     *
     * @param id 专栏ID
     * @return 专栏详情
     */
    @Operation(summary = "获取专栏详情", description = "获取专栏详情，草稿状态仅作者可见")
    @GetMapping("/{id}")
    public JsonData<ColumnDetailVO> getColumnDetail(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long currentUserId = UserContextHolder.getUserId();
        ColumnDetailVO vo = columnService.getColumnDetail(id, currentUserId);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 分页查询专栏列表
     * 公开接口，仅返回已发布的专栏
     * 支持按用户筛选、关键词搜索、排序
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    @Operation(summary = "获取专栏列表", description = "分页查询专栏列表，仅返回已发布的专栏")
    @PostMapping("/list")
    public JsonData<Page<ColumnListVO>> getColumnPage(@RequestBody ColumnQueryDTO dto) {
        Page<ColumnListVO> page = columnService.getColumnPage(dto);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取我的专栏列表
     * 包含草稿和已发布的专栏
     * 按更新时间降序排列
     *
     * @return 专栏列表
     */
    @Operation(summary = "获取我的专栏", description = "获取当前用户的专栏列表，包含草稿和已发布")
    @GetMapping("/my")
    public JsonData<List<MyColumnVO>> getMyColumns() {
        Long userId = UserContextHolder.getUserId();
        List<MyColumnVO> list = columnService.getMyColumns(userId);
        return JsonData.buildSuccess(list);
    }

    /**
     * 获取热门专栏列表
     * 根据订阅量排序，返回前5个已发布的专栏
     *
     * @return 热门专栏列表
     */
    @Operation(summary = "获取热门专栏", description = "获取热门专栏列表，根据订阅量排序")
    @GetMapping("/hot")
    public JsonData<List<ColumnListVO>> getHotColumns() {
        List<ColumnListVO> list = columnService.getHotColumns(5);
        return JsonData.buildSuccess(list);
    }
}
