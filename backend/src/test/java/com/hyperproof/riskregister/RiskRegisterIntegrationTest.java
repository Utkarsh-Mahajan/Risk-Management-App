package com.hyperproof.riskregister;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperproof.riskregister.web.dto.MitigationRequest;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RiskRegisterIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String body(Object obj) throws Exception {
        return json.writeValueAsString(obj);
    }

    @Test
    void fullLifecycle() throws Exception {
        // 1. Create risk L4×I5 → inherent 20 Critical, residual 20 Critical
        RiskRequest create = new RiskRequest("Unpatched servers", "desc",
                RiskCategory.SECURITY, "Alice", 4, 5, null, null, null);

        MvcResult created = mvc.perform(post("/api/risks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inherentScore").value(20))
                .andExpect(jsonPath("$.inherentBand").value("CRITICAL"))
                .andExpect(jsonPath("$.residualScore").value(20))
                .andExpect(jsonPath("$.residualBand").value("CRITICAL"))
                .andExpect(jsonPath("$.mitigationCount").value(0))
                .andReturn();

        long riskId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // 2. Add eff-5 mitigation → residual drops to 10 Medium, status auto-promotes Open → Mitigating
        mvc.perform(post("/api/risks/{id}/mitigations", riskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(new MitigationRequest("WAF deployed", 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.residualScore").value(10))
                .andExpect(jsonPath("$.residualBand").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("MITIGATING"));

        // 2b. Cannot force status back to Open while a mitigation is attached
        RiskRequest reopenAsOpen = new RiskRequest("Unpatched servers", "desc",
                RiskCategory.SECURITY, "Alice", 4, 5, RiskStatus.OPEN, null, null);
        mvc.perform(patch("/api/risks/{id}", riskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(reopenAsOpen)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("mitigations_exist"));

        // 3. Add second eff-5 → residual 5 Low
        mvc.perform(post("/api/risks/{id}/mitigations", riskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(new MitigationRequest("Patch management process", 5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.residualScore").value(5))
                .andExpect(jsonPath("$.residualBand").value("LOW"));

        // 4. Close risk → status Closed, residual still 5 (closing does not change score)
        RiskRequest close = new RiskRequest("Unpatched servers", "desc",
                RiskCategory.SECURITY, "Alice", 4, 5, RiskStatus.CLOSED, null, null);

        mvc.perform(patch("/api/risks/{id}", riskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(close)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closureReason").value("MITIGATED"))
                .andExpect(jsonPath("$.residualScore").value(5));

        // 4b. Adding a mitigation to a closed risk is blocked → 409, must reopen first
        mvc.perform(post("/api/risks/{id}/mitigations", riskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(new MitigationRequest("Late control", 4))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("risk_closed"));

        // 5a. New risk, try closing with no reason → 422
        RiskRequest newRisk = new RiskRequest("Orphan risk", "desc",
                RiskCategory.OPERATIONAL, "Bob", 3, 3, null, null, null);
        MvcResult r2 = mvc.perform(post("/api/risks")
                        .contentType(MediaType.APPLICATION_JSON).content(body(newRisk)))
                .andExpect(status().isCreated()).andReturn();
        long riskId2 = json.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        RiskRequest closeNoReason = new RiskRequest("Orphan risk", "desc",
                RiskCategory.OPERATIONAL, "Bob", 3, 3, RiskStatus.CLOSED, null, null);
        mvc.perform(patch("/api/risks/{id}", riskId2)
                        .contentType(MediaType.APPLICATION_JSON).content(body(closeNoReason)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("closure_reason_required"));

        // 5b. Close with RISK_ACCEPTED + justification → residual == inherent
        RiskRequest closeAccepted = new RiskRequest("Orphan risk", "desc",
                RiskCategory.OPERATIONAL, "Bob", 3, 3, RiskStatus.CLOSED,
                ClosureReason.RISK_ACCEPTED, "Accepted by management");
        mvc.perform(patch("/api/risks/{id}", riskId2)
                        .contentType(MediaType.APPLICATION_JSON).content(body(closeAccepted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residualScore").value(9))   // inherent = 3×3 = 9
                .andExpect(jsonPath("$.residualBand").value("MEDIUM"));

        // 6. Delete last mitigation of MITIGATED closed risk → 409
        // The risk has 2 mitigations; delete the second one first (allowed), then the last → 409
        MvcResult riskResp = mvc.perform(get("/api/risks/{id}", riskId)).andReturn();
        var mitigationsNode = json.readTree(riskResp.getResponse().getContentAsString()).get("mitigations");
        long firstMitigationId = mitigationsNode.get(0).get("id").asLong();
        long secondMitigationId = mitigationsNode.get(1).get("id").asLong();

        mvc.perform(delete("/api/risks/{riskId}/mitigations/{id}", riskId, secondMitigationId))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/risks/{riskId}/mitigations/{id}", riskId, firstMitigationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("closure_basis_removed"));

        // 7. List with sort=residual,desc
        mvc.perform(get("/api/risks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].residualScore").value(greaterThanOrEqualTo(
                        (int) json.readTree(
                                mvc.perform(get("/api/risks")).andReturn().getResponse().getContentAsString()
                        ).get(1).get("residualScore").asInt())));

        // 8. Validation: likelihood=6 → 400
        RiskRequest invalid = new RiskRequest("bad", "d", RiskCategory.SECURITY, "o", 6, 3, null, null, null);
        mvc.perform(post("/api/risks")
                        .contentType(MediaType.APPLICATION_JSON).content(body(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.fields.likelihood").exists());
    }
}
