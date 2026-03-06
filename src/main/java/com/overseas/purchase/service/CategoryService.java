package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.overseas.purchase.entity.Category;
import com.overseas.purchase.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final CategoryMapper categoryMapper;
    
    /**
     * 查询所有分类（包括子分类）
     */
    public List<Category> getAllCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getDeleted, 0)
                .orderByAsc(Category::getSortOrder)
        );
    }
    
    /**
     * 查询顶级分类（父分类）
     */
    public List<Category> getTopCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getDeleted, 0)
                .and(wrapper -> wrapper.eq(Category::getParentId, 0).or().isNull(Category::getParentId))
                .orderByAsc(Category::getSortOrder)
        );
    }
    
    /**
     * 根据父分类ID查询子分类
     */
    public List<Category> getSubCategories(Long parentId) {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<Category>()
                .eq(Category::getDeleted, 0)
                .eq(Category::getParentId, parentId)
                .orderByAsc(Category::getSortOrder)
        );
    }
    
    /**
     * 根据ID查询分类
     */
    public Category getCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }
    
    /**
     * 添加分类
     */
    public void addCategory(Category category) {
        categoryMapper.insert(category);
    }
    
    /**
     * 更新分类
     */
    public void updateCategory(Category category) {
        categoryMapper.updateById(category);
    }
    
    /**
     * 删除分类
     */
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }
}
