package com.experiment.ci.service;

import com.experiment.ci.model.Product;
import com.experiment.ci.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Product business logic.
 * This class is the primary target for unit testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Retrieve all products.
     * @return list of all products
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll();
    }

    /**
     * Retrieve a product by its ID.
     * @param id product ID
     * @return Optional containing the product if found
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id);
    }

    /**
     * Create a new product.
     * @param product product to create
     * @return saved product
     * @throws IllegalArgumentException if product name already exists
     */
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getName());
        if (productRepository.existsByNameIgnoreCase(product.getName())) {
            throw new IllegalArgumentException(
                "Product with name '" + product.getName() + "' already exists");
        }
        return productRepository.save(product);
    }

    /**
     * Update an existing product.
     * @param id product ID
     * @param updatedProduct updated product data
     * @return updated product
     * @throws IllegalArgumentException if product not found
     */
    public Product updateProduct(Long id, Product updatedProduct) {
        log.info("Updating product with id: {}", id);
        Product existing = productRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setDescription(updatedProduct.getDescription());

        return productRepository.save(existing);
    }

    /**
     * Delete a product by ID.
     * @param id product ID
     * @throws IllegalArgumentException if product not found
     */
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Get products by category.
     * @param category product category
     * @return list of products in that category
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category);
    }

    /**
     * Get all in-stock products (stock > 0).
     * @return list of in-stock products
     */
    @Transactional(readOnly = true)
    public List<Product> getInStockProducts() {
        return productRepository.findInStockProducts();
    }

    /**
     * Search products by keyword in name or category.
     * @param keyword search term
     * @return matching products
     */
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        return productRepository.searchByKeyword(keyword);
    }

    /**
     * Apply a percentage discount to a product price.
     * Business logic method - easy to unit test in isolation.
     * @param product product to apply discount to
     * @param discountPercent discount percentage (0-100)
     * @return discounted price
     */
    public double applyDiscount(Product product, double discountPercent) {
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100 percent");
        }
        double discountedPrice = product.getPrice() * (1 - discountPercent / 100.0);
        log.info("Applied {}% discount to '{}': {} -> {}",
            discountPercent, product.getName(), product.getPrice(), discountedPrice);
        return Math.round(discountedPrice * 100.0) / 100.0;
    }

    /**
     * Check if a product is low on stock (< threshold).
     * @param product product to check
     * @param threshold low stock threshold
     * @return true if stock is below threshold
     */
    public boolean isLowStock(Product product, int threshold) {
        return product.getStock() < threshold;
    }

    /**
     * Calculate total inventory value for a list of products.
     * @param products list of products
     * @return total value (price * stock)
     */
    public double calculateInventoryValue(List<Product> products) {
        return products.stream()
            .mapToDouble(p -> p.getPrice() * p.getStock())
            .sum();
    }
}
