package com.experiment.ci.repository;

import com.experiment.ci.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Product entity.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by category
    List<Product> findByCategory(String category);

    // Find product by name (case-insensitive)
    Optional<Product> findByNameIgnoreCase(String name);

    // Find products with price less than or equal to given value
    List<Product> findByPriceLessThanEqual(Double maxPrice);

    // Find in-stock products
    @Query("SELECT p FROM Product p WHERE p.stock > 0")
    List<Product> findInStockProducts();

    // Search by name or category
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    // Exists by name
    boolean existsByNameIgnoreCase(String name);
}
