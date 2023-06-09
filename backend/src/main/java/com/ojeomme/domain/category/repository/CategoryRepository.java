package com.ojeomme.domain.category.repository;

import com.ojeomme.domain.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryCustomRepository {

    Optional<Category> findByCategoryDepthAndCategoryName(int categoryDepth, String categoryName);

    List<Category> findByUpCategoryIdAndCategoryDepth(Long upId, int depth);
}