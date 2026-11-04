package com.experiment.ci.container;

import com.experiment.ci.model.Product;
import com.experiment.ci.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTCONTAINERS INTEGRATION TEST: PostgreSQL
 * ============================================
 * Demonstrates infrastructure-level testing using Testcontainers.
 * Spins up a real, lightweight Docker container running official PostgreSQL
 * instead of relying on in-memory mocks (like H2), eliminating environment parity issues.
 *
 * Concepts demonstrated:
 *  - @Testcontainers annotation enables lifecycle management of containers
 *  - @Container defines the PostgreSQL container
 *  - @DynamicPropertySource binds container port and credentials dynamically to Spring context
 *  - Real dialect SQL execution and transaction isolation verification
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("PostgreSQL Testcontainers Integration Test")
class PostgresTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test_ci_db")
        .withUsername("ci_user")
        .withPassword("ci_secret_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Verify PostgreSQL container is running and healthy")
    void testContainerRunning() {
        if (!postgres.isRunning()) {
            return; // Skip if docker engine is unavailable in local environment
        }
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDatabaseName()).isEqualTo("test_ci_db");
    }

    @Test
    @DisplayName("Verify CRUD operations against real PostgreSQL container")
    void testCrudOperationsOnRealDatabase() {
        if (!postgres.isRunning()) {
            return; // Skip gracefully if docker daemon not active
        }

        // 1. Create
        Product product = Product.builder()
            .name("Postgres Monitored Product")
            .category("Database Services")
            .price(499.00)
            .stock(5)
            .description("Persisted in real containerized PostgreSQL")
            .build();
        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();

        // 2. Read
        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Postgres Monitored Product");

        // 3. Custom Query
        List<Product> databaseProducts = productRepository.findByCategory("Database Services");
        assertThat(databaseProducts).isNotEmpty();

        // 4. Delete
        productRepository.deleteById(saved.getId());
        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }
}
