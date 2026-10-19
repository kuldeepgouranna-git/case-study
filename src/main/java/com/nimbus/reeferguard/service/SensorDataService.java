package com.nimbus.reeferguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbus.reeferguard.model.SensorReading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SensorDataService {
    private final ObjectMapper mapper;
    private final Resource dataset;

    public SensorDataService(ObjectMapper mapper,
            @Value("${reefer.dataset:classpath:data/sensor_readings.json}") Resource dataset) {
        this.mapper = mapper;
        this.dataset = dataset;
    }

    public List<SensorReading> loadReadings() {
        try (InputStream in = dataset.getInputStream()) {
            JsonNode readings = mapper.readTree(in).path("readings");
            List<SensorReading> result = new ArrayList<>();
            for (JsonNode r : readings) {
                result.add(new SensorReading(r.path("truck_id").asText(),
                        LocalDateTime.parse(r.path("timestamp").asText()),
                        r.path("temperature_c").asDouble(), r.path("humidity_pct").asDouble()));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load sensor dataset: " + dataset, e);
        }
    }

    public List<SensorReading> chronologicalReadings() {
        return loadReadings().stream()
                .sorted(Comparator.comparing(SensorReading::timestamp).thenComparing(SensorReading::truckId)).toList();
    }
}
