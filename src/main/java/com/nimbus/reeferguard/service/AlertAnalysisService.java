package com.nimbus.reeferguard.service;

import com.nimbus.reeferguard.config.AlertProperties;
import com.nimbus.reeferguard.model.AlertEpisode;
import com.nimbus.reeferguard.model.SensorReading;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AlertAnalysisService {
    private final SensorDataService data;
    private final AlertProperties properties;
    private final AtomicLong ids = new AtomicLong(1000);

    public AlertAnalysisService(SensorDataService data, AlertProperties properties) {
        this.data = data;
        this.properties = properties;
    }

    public Map<String, Object> analyze() {
        return analyze(data.loadReadings());
    }

    /**
     * Analyze a list of sensor readings and return a summary of alerts and statistics.
     * 
     * @param input - list of sensor readings to analyze
     * @return map containing summary of alerts and statistics
     */
    public Map<String, Object> analyze(List<SensorReading> input) {
        Map<String, List<SensorReading>> byTruck = input.stream()
                .collect(Collectors.groupingBy(SensorReading::truckId, TreeMap::new, Collectors.toList()));
        List<AlertEpisode> alerts = new ArrayList<>();
        List<Map<String, Object>> trucks = new ArrayList<>();
        int gaps = 0;
        for (var entry : byTruck.entrySet()) {
            List<SensorReading> readings = entry.getValue().stream()
                    .sorted(Comparator.comparing(SensorReading::timestamp)).toList();
            double peak = readings.stream().mapToDouble(SensorReading::temperatureC).max().orElse(0);
            int truckGaps = 0;
            for (int i = 1; i < readings.size(); i++)
                if (Duration.between(readings.get(i - 1).timestamp(), readings.get(i).timestamp())
                        .toMinutes() > properties.getMaxGapMinutes())
                    truckGaps++;
            gaps += truckGaps;
            List<SensorReading> episode = new ArrayList<>();
            for (SensorReading reading : readings) {
                if (reading.temperatureC() > properties.getThresholdC()) {
                    if (!episode.isEmpty()
                            && Duration.between(episode.get(episode.size() - 1).timestamp(), reading.timestamp())
                                    .toMinutes() > properties.getMaxGapMinutes()) {
                        addIfAlertable(entry.getKey(), episode, alerts);
                        episode.clear();
                    }
                    episode.add(reading);
                } else if (!episode.isEmpty()) {
                    addIfAlertable(entry.getKey(), episode, alerts);
                    episode.clear();
                }
            }
            if (!episode.isEmpty())
                addIfAlertable(entry.getKey(), episode, alerts);
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("truckId", entry.getKey());
            summary.put("readingCount", readings.size());
            summary.put("peakTemperatureC", round(peak));
            summary.put("alertCount", alerts.stream().filter(a -> a.truckId().equals(entry.getKey())).count());
            summary.put("dataGapCount", truckGaps);
            summary.put("latestReading", readings.isEmpty() ? null : readings.get(readings.size() - 1).timestamp());
            trucks.add(summary);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", java.time.LocalDateTime.now());
        response.put("totalReadings", input.size());
        response.put("truckCount", byTruck.size());
        response.put("alertCount", alerts.size());
        response.put("dataGapCount", gaps);
        response.put("configuration",
                Map.of("thresholdC", properties.getThresholdC(), "minDurationMinutes",
                        properties.getMinDurationMinutes(), "maxGapMinutes", properties.getMaxGapMinutes(),
                        "highSeverityC", properties.getHighSeverityC()));
        response.put("trucks", trucks);
        response.put("alerts", alerts);
        return response;
    }

    /**
     * Add an alert episode to the list if it meets the criteria for alerting.
     * @param truck - the truck ID associated with the episode
     * @param episode - the list of sensor readings that make up the episode
     * @param alerts - the list of alert episodes to add to
     */
    private void addIfAlertable(String truck, List<SensorReading> episode, List<AlertEpisode> alerts) {
        if (episode.isEmpty())
            return;
        long duration = Duration.between(episode.get(0).timestamp(), episode.get(episode.size() - 1).timestamp())
                .toMinutes();
        if (duration < properties.getMinDurationMinutes())
            return;
        double peak = episode.stream().mapToDouble(SensorReading::temperatureC).max().orElse(0);
        String severity = peak >= properties.getHighSeverityC() ? "HIGH" : "WARNING";
        alerts.add(new AlertEpisode("INC-" + ids.incrementAndGet(), truck, episode.get(0).timestamp(),
                episode.get(episode.size() - 1).timestamp(), duration, episode.size(), round(peak), severity, "ACTIVE",
                String.format(Locale.ROOT, "Temperature exceeded %.1f°C for %d minutes; %d violating readings.",
                        properties.getThresholdC(), duration, episode.size())));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
