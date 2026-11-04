package com.experiment.ci.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Product entity representing a product in the system.
 * Used to demonstrate unit, integration, and contract testing.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Category cannot be blank")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    @Column(nullable = false)
    private Double price;

    @Min(value = 0, message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock;

    @Column(length = 500)
    private String description;
}
