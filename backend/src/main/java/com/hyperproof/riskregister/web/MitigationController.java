package com.hyperproof.riskregister.web;

import com.hyperproof.riskregister.service.MitigationService;
import com.hyperproof.riskregister.web.dto.MitigationRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/risks/{riskId}/mitigations")
public class MitigationController {

    private final MitigationService mitigationService;

    public MitigationController(MitigationService mitigationService) {
        this.mitigationService = mitigationService;
    }

    @PostMapping
    public ResponseEntity<RiskResponse> create(@PathVariable Long riskId,
                                               @Valid @RequestBody MitigationRequest req) {
        RiskResponse updated = mitigationService.create(riskId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(updated.mitigations().getLast().id()).toUri();
        return ResponseEntity.created(location).body(updated);
    }

    @PatchMapping("/{id}")
    public RiskResponse update(@PathVariable Long riskId,
                               @PathVariable Long id,
                               @Valid @RequestBody MitigationRequest req) {
        return mitigationService.update(riskId, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long riskId, @PathVariable Long id) {
        mitigationService.delete(riskId, id);
        return ResponseEntity.noContent().build();
    }
}
