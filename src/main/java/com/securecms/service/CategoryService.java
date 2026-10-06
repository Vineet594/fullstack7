package com.securecms.service;

import com.securecms.cache.CacheNames;
import com.securecms.dto.CategoryRequest;
import com.securecms.dto.CategoryResponse;
import com.securecms.entity.Category;
import com.securecms.exception.ResourceConflictException;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caching vocabulary used here:
 *  - cache MISS : value not in cache -> method body runs (database is queried) -> result stored
 *  - cache HIT  : value found in cache -> method body is skipped (0 SQL queries)
 *  - invalidation: create/update/delete remove or refresh cached entries so users never see stale data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        log.info("Cache MISS - loading all categories from the database");
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY, key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        log.info("Cache MISS - loading category {} from the database", id);
        return CategoryResponse.from(loadCategory(id));
    }

    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("CATEGORY_ALREADY_EXISTS", "Category '" + name + "' already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    // @CachePut refreshes the single-category entry; the list cache and cached posts (which show the
    // category name) are invalidated.
    @Caching(
            put = @CachePut(cacheNames = CacheNames.CATEGORY, key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true),
                    @CacheEvict(cacheNames = CacheNames.POST, allEntries = true)
            })
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = loadCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("CATEGORY_ALREADY_EXISTS", "Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(category);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORY, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.CATEGORIES, allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        Category category = loadCategory(id);
        if (postRepository.existsByCategoryId(id)) {
            throw new ResourceConflictException("CATEGORY_IN_USE",
                    "Category cannot be deleted because posts still use it");
        }
        categoryRepository.delete(category);
    }

    private Category loadCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
