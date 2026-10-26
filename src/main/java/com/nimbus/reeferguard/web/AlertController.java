package com.nimbus.reeferguard.web;

import com.nimbus.reeferguard.config.AlertProperties;
import com.nimbus.reeferguard.service.AlertAnalysisService;
import com.nimbus.reeferguard.service.SimulationService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for alert analysis and simulation endpoints.
 * Provides endpoints to analyze sensor readings, manage simulation state, and update alerting rules.
 */
@RestController
@RequestMapping("/api")
@Validated
public class AlertController {
    private final AlertAnalysisService analyzer;
    private final SimulationService simulation;
    private final AlertProperties props;

    public AlertController(AlertAnalysisService analyzer, SimulationService simulation, AlertProperties props) {
        this.analyzer = analyzer;
        this.simulation = simulation;
        this.props = props;
    }

    @GetMapping("/analysis")
    public Map<String, Object> analysis() {
        return analyzer.analyze();
    }

    @GetMapping("/simulation")
    public Map<String, Object> simulation() {
        return simulation.current();
    }

    @PostMapping("/simulation/start")
    public Map<String, Object> start() {
        return simulation.start();
    }

    @PostMapping("/simulation/stop")
    public Map<String, Object> stop() {
        return simulation.stop();
    }

    @PostMapping("/simulation/reset")
    public Map<String, Object> reset() {
        return simulation.reset();
    }

    /**
     *  Configuration endpoints 
     * */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("thresholdC", props.getThresholdC(), "minDurationMinutes", props.getMinDurationMinutes(),
                "maxGapMinutes", props.getMaxGapMinutes(), "highSeverityC", props.getHighSeverityC(),
                "simulationDelayMs", props.getSimulationDelayMs());
    }

    /**
     * Update the alerting rules for subsequent analyses. The new rules will not affect any alerts that have already been generated.
     * 
     * @param thresholdC
     * @param minDurationMinutes
     * @param maxGapMinutes
     * @return
     */
    @PutMapping("/config")
    public Map<String, Object> updateConfig(@RequestParam @DecimalMin("1.0") @DecimalMax("30.0") double thresholdC,
            @RequestParam @Min(5) @Max(120) int minDurationMinutes, @RequestParam @Min(5) @Max(60) int maxGapMinutes) {
        props.setThresholdC(thresholdC);
        props.setMinDurationMinutes(minDurationMinutes);
        props.setMaxGapMinutes(maxGapMinutes);
        Map<String, Object> result = new LinkedHashMap<>(config());
        result.put("message", "Rules updated for subsequent analyses");
        return result;
    }

    /**
     * Health check endpoint for monitoring the service status. Returns a simple JSON object indicating the service is up and running.
     * @return
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "ReeferGuard", "mode", "local-demo");
    }
}
