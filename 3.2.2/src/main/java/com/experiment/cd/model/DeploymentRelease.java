package com.experiment.cd.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity tracking deployment releases across environments (staging, production).
 */
@Entity
@Table(name = "deployment_releases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private String commitSha;

    @Column(nullable = false)
    private String status;

    private LocalDateTime deployedAt;

    @PrePersist
    public void prePersist() {
        if (this.deployedAt == null) {
            this.deployedAt = LocalDateTime.now();
        }
    }
}
