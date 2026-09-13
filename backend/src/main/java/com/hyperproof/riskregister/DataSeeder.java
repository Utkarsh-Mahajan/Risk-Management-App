package com.hyperproof.riskregister;

import com.hyperproof.riskregister.domain.*;
import com.hyperproof.riskregister.repository.RiskRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
class DataSeeder implements ApplicationRunner {

    private final RiskRepository risks;

    DataSeeder(RiskRepository risks) {
        this.risks = risks;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (risks.count() > 0) return;

        Risk unpatched = seed("Unpatched production servers",
                "Critical infrastructure running OS versions past end-of-life with known CVEs unaddressed.",
                RiskCategory.SECURITY, "Alice Chen", 5, 5, RiskStatus.OPEN);

        Risk supplier = seed("Supplier concentration risk",
                "Three core services depend on a single cloud vendor with no documented failover.",
                RiskCategory.OPERATIONAL, "Bob Patel", 4, 5, RiskStatus.OPEN);

        Risk gdpr = seed("GDPR data retention gap",
                "Customer PII retained beyond permitted period due to missing automated purge.",
                RiskCategory.COMPLIANCE, "Clara Osei", 4, 4, RiskStatus.MITIGATING);
        gdpr.getMitigations().add(new Mitigation(gdpr, "Automated retention policy script running nightly on all PII tables", 4));
        gdpr.getMitigations().add(new Mitigation(gdpr, "DPO audit of data categories and retention schedules completed", 3));

        Risk insider = seed("Insider trading controls",
                "No automated detection for unusual trading patterns ahead of earnings calls.",
                RiskCategory.FINANCIAL, "David Kim", 3, 5, RiskStatus.OPEN);

        Risk market = seed("Market expansion regulatory",
                "Entry into EU market may trigger MiFID II obligations not currently supported.",
                RiskCategory.STRATEGIC, "Eve Nakamura", 3, 4, RiskStatus.OPEN);

        Risk phishing = seed("Phishing susceptibility",
                "Simulated phishing campaigns show 22% click-through rate; no MFA on email.",
                RiskCategory.SECURITY, "Alice Chen", 4, 3, RiskStatus.MITIGATING);
        phishing.getMitigations().add(new Mitigation(phishing, "Mandatory phishing awareness training rolled out to all staff", 3));

        Risk manual = seed("Undocumented manual process",
                "Month-end close depends on institutional knowledge held by one person.",
                RiskCategory.OPERATIONAL, "Bob Patel", 3, 3, RiskStatus.OPEN);

        Risk legacy = seed("Legacy payment processor",
                "PCI-DSS scope creep from unmigrated v1 API; decommission delayed 18 months.",
                RiskCategory.COMPLIANCE, "Clara Osei", 2, 4, RiskStatus.CLOSED);
        legacy.setClosureReason(ClosureReason.RISK_ACCEPTED);
        legacy.setClosureJustification("Decommission timeline approved by CTO. Risk formally accepted until Q2 migration completes. Compensating controls in place via network segmentation.");
        legacy.setClosedAt(Instant.now());

        risks.saveAll(List.of(unpatched, supplier, gdpr, insider, market, phishing, manual, legacy));
    }

    private Risk seed(String title, String description, RiskCategory category,
                      String owner, int likelihood, int impact, RiskStatus status) {
        Risk r = new Risk(title, description, category, owner, likelihood, impact);
        r.setStatus(status);
        return r;
    }
}
