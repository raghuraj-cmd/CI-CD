package com.experiment.ci.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CONTRACT TESTS - Pact Consumer Side
 * ======================================
 * Defines the contract expected by frontend/client consumer from product-api.
 * Generates target/pacts/frontend-client-product-api.json.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product-api", pactVersion = PactSpecVersion.V3)
@DisplayName("Product API - Consumer Contract Tests")
class ProductConsumerContractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    // ------------------------------------------------------------------
    // CONTRACT 1: Get all products (Root JSON Array)
    // ------------------------------------------------------------------
    @Pact(consumer = "frontend-client", provider = "product-api")
    public RequestResponsePact getAllProductsPact(PactDslWithProvider builder) {
        return builder
            .given("products exist in the system")
            .uponReceiving("a GET request for all products")
                .path("/api/products")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(PactDslJsonArray.arrayMinLike(1)
                    .integerType("id", 1)
                    .stringType("name", "Laptop")
                    .stringType("category", "Electronics")
                    .decimalType("price", 999.99)
                    .integerType("stock", 10)
                    .stringType("description", "High-end laptop")
                    .closeObject()
                )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getAllProductsPact")
    @DisplayName("Consumer should handle GET /api/products response correctly")
    void testGetAllProducts(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/products";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Laptop");
        assertThat(response.getBody()).contains("Electronics");
    }

    // ------------------------------------------------------------------
    // CONTRACT 2: Get product by ID
    // ------------------------------------------------------------------
    @Pact(consumer = "frontend-client", provider = "product-api")
    public RequestResponsePact getProductByIdPact(PactDslWithProvider builder) {
        return builder
            .given("a product with id 1 exists")
            .uponReceiving("a GET request for product with id 1")
                .path("/api/products/1")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .integerType("id", 1)
                    .stringType("name", "Laptop")
                    .stringType("category", "Electronics")
                    .decimalType("price", 999.99)
                    .integerType("stock", 10)
                    .stringType("description", "High-end laptop")
                )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getProductByIdPact")
    @DisplayName("Consumer should handle GET /api/products/1 response correctly")
    void testGetProductById(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/products/1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Laptop");
    }

    // ------------------------------------------------------------------
    // CONTRACT 3: Create product
    // ------------------------------------------------------------------
    @Pact(consumer = "frontend-client", provider = "product-api")
    public RequestResponsePact createProductPact(PactDslWithProvider builder) {
        return builder
            .given("product creation is available")
            .uponReceiving("a POST request to create a new product")
                .path("/api/products")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .stringType("name", "Test Product")
                    .stringType("category", "Test Category")
                    .decimalType("price", 50.0)
                    .integerType("stock", 20)
                    .stringType("description", "Product description")
                )
            .willRespondWith()
                .status(201)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .integerType("id", 100)
                    .stringType("name", "Test Product")
                    .stringType("category", "Test Category")
                    .decimalType("price", 50.0)
                    .integerType("stock", 20)
                    .stringType("description", "Product description")
                )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createProductPact")
    @DisplayName("Consumer should handle POST /api/products response correctly")
    void testCreateProduct(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/products";
        String body = "{\"name\":\"Test Product\",\"category\":\"Test Category\",\"price\":50.0,\"stock\":20,\"description\":\"Product description\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Test Product");
    }

    // ------------------------------------------------------------------
    // CONTRACT 4: Health check endpoint
    // ------------------------------------------------------------------
    @Pact(consumer = "frontend-client", provider = "product-api")
    public RequestResponsePact healthCheckPact(PactDslWithProvider builder) {
        return builder
            .given("service is running")
            .uponReceiving("a GET request for health status")
                .path("/api/products/health")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                    .stringType("status", "UP")
                    .stringType("service", "product-api")
                )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "healthCheckPact")
    @DisplayName("Consumer should handle GET /api/products/health response correctly")
    void testHealthCheck(MockServer mockServer) {
        String url = mockServer.getUrl() + "/api/products/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
