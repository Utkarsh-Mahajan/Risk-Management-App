package com.hyperproof.riskregister.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "mitigations")
public class Mitigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_id", nullable = false)
    private Risk risk;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @NotNull
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer effectiveness;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Mitigation() {}

    public Mitigation(Risk risk, String description, Integer effectiveness) {
        this.risk = risk;
        this.description = description;
        this.effectiveness = effectiveness;
    }

    public Long getId() { return id; }
    public Risk getRisk() { return risk; }
    public String getDescription() { return description; }
    public Integer getEffectiveness() { return effectiveness; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDescription(String description) { this.description = description; }
    public void setEffectiveness(Integer effectiveness) { this.effectiveness = effectiveness; }
}
