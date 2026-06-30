package com.personblog.article.controller.Catagory;

import com.personblog.article.service.ICategoryService;
import com.personblog.article.vo.Category.CategoryVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Tag(name = "分类控制器",description = "描述分类")
public class CategoryController {
    private final ICategoryService categoryService;
    @GetMapping
    @Operation(description = "获取分类")
    public JsonData<List<CategoryVO>> getCategory(){
       List<CategoryVO> res = categoryService.getCategory();
       return JsonData.buildSuccess(res);
    }


}
