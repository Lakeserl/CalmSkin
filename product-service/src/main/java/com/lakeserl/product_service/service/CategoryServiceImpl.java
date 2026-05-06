package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateCategoryRequest;
import com.lakeserl.product_service.dto.request.UpdateCategoryRequest;
import com.lakeserl.product_service.dto.response.CategoryDTO;
import com.lakeserl.product_service.dto.response.CategoryTreeDTO;
import com.lakeserl.product_service.entity.Category;
import com.lakeserl.product_service.exception.CategoryNotFoundException;
import com.lakeserl.product_service.exception.DuplicateSlugException;
import com.lakeserl.product_service.mapper.CategoryMapper;
import com.lakeserl.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Override
    @Cacheable(value = "categories:all", unless = "#result == null")
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "categories:tree", unless = "#result == null")
    public List<CategoryTreeDTO> getCategoryTree() {
        return categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(categoryMapper::toTreeDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with slug: " + slug));
        
        if (!category.getIsActive()) {
            throw new CategoryNotFoundException("Category is not active: " + slug);
        }
        
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories:all", "categories:tree"}, allEntries = true)
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        String slug = generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateSlugException(slug);
        }

        Category category = categoryMapper.toEntity(request);
        category.setSlug(slug);

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getParentId()));
            category.setParent(parent);
        }

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories:all", "categories:tree"}, allEntries = true)
    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        categoryMapper.updateEntityFromRequest(request, category);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                throw new DuplicateSlugException(newSlug);
            }
            category.setSlug(newSlug);
        }

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getParentId()));
            if (parent.getId().equals(category.getId())) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            category.setParent(parent);
        } else if (request.getParentId() == null && category.getParent() != null) {
            // How to handle removing parent? Typically passing null doesn't do it in mapstruct if we ignore nulls.
            // But if we specifically want to detach, we'd need explicit logic. Let's assume updating parent is handled explicitly.
        }

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories:all", "categories:tree"}, allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        // Soft delete
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories:all", "categories:tree"}, allEntries = true)
    public CategoryDTO updateCategoryStatus(Long id, Boolean isActive) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        category.setIsActive(isActive);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
