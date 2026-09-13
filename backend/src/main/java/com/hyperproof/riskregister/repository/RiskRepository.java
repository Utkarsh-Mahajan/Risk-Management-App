package com.hyperproof.riskregister.repository;

import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.RiskCategory;
import com.hyperproof.riskregister.domain.RiskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    @EntityGraph(attributePaths = "mitigations")
    @Query("select r from Risk r")
    List<Risk> findAllWithMitigations();

    @EntityGraph(attributePaths = "mitigations")
    @Query("select r from Risk r where r.category = :category")
    List<Risk> findByCategoryWithMitigations(RiskCategory category);

    @EntityGraph(attributePaths = "mitigations")
    @Query("select r from Risk r where r.status = :status")
    List<Risk> findByStatusWithMitigations(RiskStatus status);

    @EntityGraph(attributePaths = "mitigations")
    @Query("select r from Risk r where r.category = :category and r.status = :status")
    List<Risk> findByCategoryAndStatusWithMitigations(RiskCategory category, RiskStatus status);

    @EntityGraph(attributePaths = "mitigations")
    Optional<Risk> findWithMitigationsById(Long id);
}
