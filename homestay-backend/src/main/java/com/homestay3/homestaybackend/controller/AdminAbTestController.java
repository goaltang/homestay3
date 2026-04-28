package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.AbExperiment;
import com.homestay3.homestaybackend.service.AbTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ab-tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAbTestController {

    private final AbTestService abTestService;

    @GetMapping
    public ResponseEntity<?> getExperiments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(abTestService.getExperiments(status, pageable));
    }

    @PostMapping
    public ResponseEntity<?> createExperiment(@RequestBody Map<String, Object> request) {
        try {
            AbExperiment experiment = new AbExperiment();
            experiment.setName((String) request.get("name"));
            experiment.setDescription((String) request.get("description"));
            experiment.setExperimentType((String) request.get("experimentType"));
            experiment.setTargetId(Long.valueOf(request.get("targetId").toString()));
            experiment.setTrafficPercent((Integer) request.getOrDefault("trafficPercent", 100));
            experiment.setPrimaryMetric((String) request.getOrDefault("primaryMetric", "CONVERSION"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> variants = (List<Map<String, Object>>) request.get("variants");
            return ResponseEntity.ok(abTestService.createExperiment(experiment, variants));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExperiment(@PathVariable Long id, @RequestBody AbExperiment experiment) {
        try {
            return ResponseEntity.ok(abTestService.updateExperiment(id, experiment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExperiment(@PathVariable Long id) {
        try {
            abTestService.deleteExperiment(id);
            return ResponseEntity.ok(Map.of("message", "实验已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startExperiment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(abTestService.startExperiment(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopExperiment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(abTestService.stopExperiment(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExperimentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(abTestService.getExperimentDetail(id));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<?> getExperimentReport(@PathVariable Long id) {
        return ResponseEntity.ok(abTestService.getExperimentReport(id));
    }
}
