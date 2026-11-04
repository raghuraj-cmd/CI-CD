package com.experiment.ci.unit;

import com.experiment.ci.model.Product;
import com.experiment.ci.repository.ProductRepository;
import com.experiment.ci.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS for ProductService
 * ==============================
 * These tests isolate the service layer using Mockito mocks.
 * No Spring context is loaded — pure JUnit 5 unit tests.
 *
 * Testing concepts demonstrated:
 *  - @Mock + @InjectMocks (Mockito)
 *  - Stubbing with when(...).thenReturn(...)
 *  - Verification with verify(...)
 *  - Exception testing with assertThatThrownBy(...)
 *  - Parameterized tests
 *  - Nested test classes for grouping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // Helper method to build a test product
    private Product buildProduct(Long id, String name, String category, double price, int stock) {
        return Product.builder()
            .id(id).name(name).category(category).price(price).stock(stock).build();
    }

    // ===========================================================
    // SECTION 1: CRUD Operations
    // ===========================================================
    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("Should return all products")
        void testGetAllProducts() {
            // ARRANGE
            List<Product> mockProducts = Arrays.asList(
                buildProduct(1L, "Laptop", "Electronics", 999.99, 10),
                buildProduct(2L, "Headphones", "Electronics", 149.99, 25)
            );
            when(productRepository.findAll()).thenReturn(mockProducts);

            // ACT
            List<Product> result = productService.getAllProducts();

            // ASSERT
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Laptop");
            assertThat(result.get(1).getName()).isEqualTo("Headphones");
            verify(productRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return product by ID when found")
        void testGetProductById_Found() {
            Product product = buildProduct(1L, "Laptop", "Electronics", 999.99, 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            Optional<Product> result = productService.getProductById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Laptop");
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty Optional when product not found")
        void testGetProductById_NotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            Optional<Product> result = productService.getProductById(99L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should create product successfully")
        void testCreateProduct_Success() {
            Product newProduct = buildProduct(null, "Mouse", "Electronics", 29.99, 50);
            Product savedProduct = buildProduct(1L, "Mouse", "Electronics", 29.99, 50);

            when(productRepository.existsByNameIgnoreCase("Mouse")).thenReturn(false);
            when(productRepository.save(newProduct)).thenReturn(savedProduct);

            Product result = productService.createProduct(newProduct);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Mouse");
            verify(productRepository).save(newProduct);
        }

        @Test
        @DisplayName("Should throw exception when creating duplicate product name")
        void testCreateProduct_DuplicateName() {
            Product product = buildProduct(null, "Laptop", "Electronics", 999.99, 10);
            when(productRepository.existsByNameIgnoreCase("Laptop")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(product))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update product successfully")
        void testUpdateProduct_Success() {
            Product existing = buildProduct(1L, "Old Laptop", "Electronics", 800.0, 5);
            Product updated = buildProduct(null, "New Laptop", "Electronics", 1200.0, 8);
            Product saved = buildProduct(1L, "New Laptop", "Electronics", 1200.0, 8);

            when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(productRepository.save(any(Product.class))).thenReturn(saved);

            Product result = productService.updateProduct(1L, updated);

            assertThat(result.getName()).isEqualTo("New Laptop");
            assertThat(result.getPrice()).isEqualTo(1200.0);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent product")
        void testUpdateProduct_NotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(99L,
                buildProduct(null, "X", "Y", 1.0, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should delete product successfully")
        void testDeleteProduct_Success() {
            when(productRepository.existsById(1L)).thenReturn(true);
            doNothing().when(productRepository).deleteById(1L);

            assertThatCode(() -> productService.deleteProduct(1L))
                .doesNotThrowAnyException();

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void testDeleteProduct_NotFound() {
            when(productRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

            verify(productRepository, never()).deleteById(any());
        }
    }

    // ===========================================================
    // SECTION 2: Business Logic
    // ===========================================================
    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @ParameterizedTest(name = "Discount {0}% on $100.00 should yield ${1}")
        @CsvSource({
            "0,   100.0",
            "10,  90.0",
            "25,  75.0",
            "50,  50.0",
            "100, 0.0"
        })
        @DisplayName("Should apply discount correctly")
        void testApplyDiscount(double discountPercent, double expectedPrice) {
            Product product = buildProduct(1L, "Item", "Test", 100.0, 5);
            double result = productService.applyDiscount(product, discountPercent);
            assertThat(result).isEqualTo(expectedPrice);
        }

        @ParameterizedTest(name = "Discount {0}% is invalid")
        @ValueSource(doubles = {-1, -10, 101, 200})
        @DisplayName("Should throw exception for invalid discount percentage")
        void testApplyDiscount_Invalid(double invalidDiscount) {
            Product product = buildProduct(1L, "Item", "Test", 100.0, 5);
            assertThatThrownBy(() -> productService.applyDiscount(product, invalidDiscount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount must be between 0 and 100");
        }

        @Test
        @DisplayName("Should detect low stock correctly")
        void testIsLowStock() {
            Product product = buildProduct(1L, "Item", "Test", 10.0, 3);
            assertThat(productService.isLowStock(product, 5)).isTrue();
            assertThat(productService.isLowStock(product, 3)).isFalse();
            assertThat(productService.isLowStock(product, 2)).isFalse();
        }

        @Test
        @DisplayName("Should calculate total inventory value correctly")
        void testCalculateInventoryValue() {
            List<Product> products = Arrays.asList(
                buildProduct(1L, "A", "X", 10.0, 5),   // 50.0
                buildProduct(2L, "B", "X", 20.0, 3),   // 60.0
                buildProduct(3L, "C", "X", 5.0, 10)    // 50.0
            );
            double total = productService.calculateInventoryValue(products);
            assertThat(total).isEqualTo(160.0);
        }

        @Test
        @DisplayName("Should return 0 for empty product list inventory value")
        void testCalculateInventoryValue_Empty() {
            double total = productService.calculateInventoryValue(List.of());
            assertThat(total).isEqualTo(0.0);
        }
    }

    // ===========================================================
    // SECTION 3: Query Methods
    // ===========================================================
    @Nested
    @DisplayName("Query Method Tests")
    class QueryTests {

        @Test
        @DisplayName("Should return products filtered by category")
        void testGetProductsByCategory() {
            List<Product> electronics = Arrays.asList(
                buildProduct(1L, "Laptop", "Electronics", 999.99, 10),
                buildProduct(2L, "Phone", "Electronics", 599.99, 15)
            );
            when(productRepository.findByCategory("Electronics")).thenReturn(electronics);

            List<Product> result = productService.getProductsByCategory("Electronics");

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> p.getCategory().equals("Electronics"));
        }

        @Test
        @DisplayName("Should return only in-stock products")
        void testGetInStockProducts() {
            List<Product> inStock = List.of(
                buildProduct(1L, "Laptop", "Electronics", 999.99, 5)
            );
            when(productRepository.findInStockProducts()).thenReturn(inStock);

            List<Product> result = productService.getInStockProducts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStock()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should search products by keyword")
        void testSearchProducts() {
            List<Product> searchResults = List.of(
                buildProduct(1L, "Laptop Pro", "Electronics", 1299.99, 5)
            );
            when(productRepository.searchByKeyword("laptop")).thenReturn(searchResults);

            List<Product> result = productService.searchProducts("laptop");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).containsIgnoringCase("laptop");
        }
    }
}
