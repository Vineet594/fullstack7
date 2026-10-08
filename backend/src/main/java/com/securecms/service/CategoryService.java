package com.securecms.service;

import com.securecms.dto.CategoryRequest;
import com.securecms.dto.CategoryResponse;
import com.securecms.entity.Category;
import com.securecms.exception.ConflictException;
import com.securecms.exception.ResourceNotFoundException;
import com.securecms.repository.CategoryRepository;
import com.securecms.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caching vocabulary:
 * - cache MISS: value not in cache -> method runs, DB is queried, result is stored (@Cacheable)
 * - cache HIT : value found -> method body is skipped, no DB call
 * - invalidation: @CacheEvict removes stale entries, @CachePut refreshes an entry after an update
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        log.info("Cache MISS - loading categories from database");
        return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Cacheable(value = "category", key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        log.info("Cache MISS - loading category {} from database", id);
        return CategoryResponse.from(findOrThrow(id));
    }

    @CacheEvict(value = "categories", key = "'all'")
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("CATEGORY_EXISTS", "A category with this name already exists");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(request.name().trim()).description(request.description()).build());
        return CategoryResponse.from(saved);
    }

    @Caching(
            put = @CachePut(value = "category", key = "#id"),
            evict = {@CacheEvict(value = "categories", key = "'all'"),
                     @CacheEvict(value = "posts", allEntries = true)})   // cached posts contain the category name
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("CATEGORY_EXISTS", "A category with this name already exists");
        }
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Caching(evict = {
            @CacheEvict(value = "category", key = "#id"),
            @CacheEvict(value = "categories", key = "'all'"),
            @CacheEvict(value = "posts", allEntries = true)})
    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);
        if (postRepository.existsByCategoryId(id)) {
            throw new ConflictException("CATEGORY_IN_USE", "Category still has posts and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "Category not found with id " + id));
    }
}
