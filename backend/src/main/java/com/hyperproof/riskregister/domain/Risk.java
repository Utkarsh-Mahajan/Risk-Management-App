package com.hyperproof.riskregister.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risks")
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskCategory category;

    @NotBlank
    @Column(nullable = false)
    private String owner;

    @NotNull
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer likelihood;

    @NotNull
    @Min(1) @Max(5)
    @Column(nullable = false)
    private Integer impact;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskStatus status = RiskStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private ClosureReason closureReason;

    @Column(columnDefinition = "TEXT")
    private String closureJustification;

    private Instant closedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "risk", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mitigation> mitigations = new ArrayList<>();

    protected Risk() {}

    public Risk(String title, String description, RiskCategory category, String owner,
                Integer likelihood, Integer impact) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.owner = owner;
        this.likelihood = likelihood;
        this.impact = impact;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public RiskCategory getCategory() { return category; }
    public String getOwner() { return owner; }
    public Integer getLikelihood() { return likelihood; }
    public Integer getImpact() { return impact; }
    public RiskStatus getStatus() { return status; }
    public ClosureReason getClosureReason() { return closureReason; }
    public String getClosureJustification() { return closureJustification; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Mitigation> getMitigations() { return mitigations; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(RiskCategory category) { this.category = category; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setLikelihood(Integer likelihood) { this.likelihood = likelihood; }
    public void setImpact(Integer impact) { this.impact = impact; }
    public void setStatus(RiskStatus status) { this.status = status; }
    public void setClosureReason(ClosureReason closureReason) { this.closureReason = closureReason; }
    public void setClosureJustification(String closureJustification) { this.closureJustification = closureJustification; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
