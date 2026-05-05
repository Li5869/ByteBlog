package com.personblog.article.controller.Admin;

import com.personblog.article.entity.Category;
import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.AdminCategoryVO;
import com.personblog.common.adminLog.RecordLog;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端分类管理控制器
 * 支持分类的 CRUD 和排序管理
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Tag(name = "管理端-分类管理", description = "管理后台的分类管理接口")
public class AdminCategoryController {

    private final ICategoryService categoryService;

    /**
     * 获取分类列表
     * 按排序字段升序返回
     */
    @Operation(summary = "获取分类列表", description = "获取全部分类列表，按排序字段升序返回")
    @GetMapping
    public JsonData<List<AdminCategoryVO>> getCategoryList() {
        List<AdminCategoryVO> list = categoryService.getAdminCategoryList();
        return JsonData.buildSuccess(list);
    }

    /**
     * 创建分类
     */
    @RecordLog(Type = "create", businessType = "category", description = "创建分类")
    @Operation(summary = "创建分类", description = "新增一个分类")
    @PostMapping
    public JsonData<AdminCategoryVO> createCategory(@RequestBody Category category) {
        AdminCategoryVO vo = categoryService.createCategory(category);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 更新分类
     */
    @RecordLog(Type = "update", businessType = "category", description = "更新分类")
    @Operation(summary = "更新分类", description = "编辑已有分类的名称和排序")
    @PutMapping("/{id}")
    public JsonData<Void> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @RequestBody Category category) {
        category.setId(id);
        categoryService.updateCategoryByAdmin(category);
        return JsonData.buildSuccess();
    }

    /**
     * 删除分类
     * 如果分类下有文章则不允许删除
     */
    @RecordLog(Type = "delete", businessType = "category", description = "删除分类")
    @Operation(summary = "删除分类", description = "删除指定分类（如果分类下有文章则不允许删除）")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        categoryService.deleteCategoryByAdmin(id);
        return JsonData.buildSuccess();
    }
}
