package com.hyperproof.riskregister.web;

import com.hyperproof.riskregister.domain.RiskCategory;
import com.hyperproof.riskregister.domain.RiskStatus;
import com.hyperproof.riskregister.service.RiskService;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping
    public List<RiskResponse> list(
            @RequestParam(required = false) RiskCategory category,
            @RequestParam(required = false) RiskStatus status) {
        return riskService.list(category, status);
    }

    @GetMapping("/{id}")
    public RiskResponse get(@PathVariable Long id) {
        return riskService.get(id);
    }

    @PostMapping
    public ResponseEntity<RiskResponse> create(@Valid @RequestBody RiskRequest req) {
        RiskResponse created = riskService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    public RiskResponse update(@PathVariable Long id, @Valid @RequestBody RiskRequest req) {
        return riskService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        riskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
