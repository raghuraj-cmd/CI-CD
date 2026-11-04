package com.experiment.ci.integration;

import com.experiment.ci.model.Product;
import com.experiment.ci.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * INTEGRATION TESTS using MockMvc
 * =================================
 * Tests the full HTTP request/response cycle for REST endpoints.
 * Uses the complete Spring context but mocks the HTTP layer.
 *
 * Testing concepts demonstrated:
 *  - @SpringBootTest + @AutoConfigureMockMvc
 *  - MockMvc for HTTP API testing
 *  - JSON path assertions (jsonPath)
 *  - HTTP status code verification
 *  - Full request/response body testing
 *  - @BeforeEach setup and @AfterEach teardown
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProductController Integration Tests (MockMvc)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        savedProduct = productRepository.save(Product.builder()
            .name("Integration Test Laptop")
            .category("Electronics")
            .price(999.99)
            .stock(10)
            .description("A high-performance laptop for testing")
            .build());
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    // -------------------------------------------------------
    // GET ALL PRODUCTS
    // -------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("GET /api/products - Should return all products with 200 OK")
    void testGetAllProducts() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("Integration Test Laptop")))
            .andExpect(jsonPath("$[0].category", is("Electronics")))
            .andExpect(jsonPath("$[0].price", is(999.99)));
    }

    // -------------------------------------------------------
    // GET PRODUCT BY ID
    // -------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("GET /api/products/{id} - Should return product with 200 OK when found")
    void testGetProductById_Found() throws Exception {
        mockMvc.perform(get("/api/products/{id}", savedProduct.getId()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(savedProduct.getId().intValue())))
            .andExpect(jsonPath("$.name", is("Integration Test Laptop")))
            .andExpect(jsonPath("$.stock", is(10)));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/products/{id} - Should return 404 when not found")
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 9999L))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // CREATE PRODUCT
    // -------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("POST /api/products - Should create product with 201 Created")
    void testCreateProduct_Success() throws Exception {
        Product newProduct = Product.builder()
            .name("New Gaming Mouse")
            .category("Peripherals")
            .price(79.99)
            .stock(30)
            .description("RGB gaming mouse")
            .build();

        ResultActions result = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
            .andDo(print());

        result.andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("New Gaming Mouse")))
            .andExpect(jsonPath("$.price", is(79.99)))
            .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/products - Should return 409 Conflict for duplicate name")
    void testCreateProduct_DuplicateName() throws Exception {
        Product duplicate = Product.builder()
            .name("Integration Test Laptop")   // same name as seeded product
            .category("Electronics")
            .price(1200.0)
            .stock(5)
            .build();

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/products - Should return 400 for invalid payload (missing name)")
    void testCreateProduct_InvalidPayload() throws Exception {
        Product invalid = Product.builder()
            .category("Electronics")
            .price(999.99)
            .stock(5)
            .build(); // name is missing

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------
    // UPDATE PRODUCT
    // -------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("PUT /api/products/{id} - Should update product with 200 OK")
    void testUpdateProduct_Success() throws Exception {
        Product updated = Product.builder()
            .name("Updated Laptop Pro")
            .category("Electronics")
            .price(1299.99)
            .stock(7)
            .description("Updated description")
            .build();

        mockMvc.perform(put("/api/products/{id}", savedProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Updated Laptop Pro")))
            .andExpect(jsonPath("$.price", is(1299.99)));
    }

    // -------------------------------------------------------
    // DELETE PRODUCT
    // -------------------------------------------------------
    @Test
    @Order(8)
    @DisplayName("DELETE /api/products/{id} - Should delete product with 204 No Content")
    void testDeleteProduct_Success() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", savedProduct.getId()))
            .andDo(print())
            .andExpect(status().isNoContent());

        // Verify it no longer exists
        mockMvc.perform(get("/api/products/{id}", savedProduct.getId()))
            .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------
    // CATEGORY FILTER
    // -------------------------------------------------------
    @Test
    @Order(9)
    @DisplayName("GET /api/products/category/{category} - Should filter by category")
    void testGetByCategory() throws Exception {
        mockMvc.perform(get("/api/products/category/{category}", "Electronics"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].category", is("Electronics")));
    }

    // -------------------------------------------------------
    // SEARCH
    // -------------------------------------------------------
    @Test
    @Order(10)
    @DisplayName("GET /api/products/search?keyword= - Should return matching products")
    void testSearchProducts() throws Exception {
        mockMvc.perform(get("/api/products/search").param("keyword", "Laptop"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // -------------------------------------------------------
    // HEALTH CHECK
    // -------------------------------------------------------
    @Test
    @Order(11)
    @DisplayName("GET /api/products/health - Should return UP status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/products/health"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.service", is("product-api")));
    }
}
