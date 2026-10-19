package com.nimbus.reeferguard;

import com.nimbus.reeferguard.config.AlertProperties;
import com.nimbus.reeferguard.model.SensorReading;
import com.nimbus.reeferguard.service.AlertAnalysisService;
import com.nimbus.reeferguard.service.SensorDataService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AlertAnalysisServiceTest {
    private AlertAnalysisService service(AlertProperties p) { return new AlertAnalysisService(new SensorDataService(new ObjectMapper(), new ClassPathResource("data/sensor_readings.json")), p); }
    private SensorReading r(String time, double temp) { return new SensorReading("T-TEST", LocalDateTime.parse("2026-01-01T"+time), temp, 50); }
    @Test void suppressesBriefSpike() {
        AlertProperties p = new AlertProperties();
        Map<String,Object> out = service(p).analyze(List.of(r("08:00:00",6),r("08:05:00",9),r("08:10:00",6)));
        assertEquals(0, ((List<?>)out.get("alerts")).size());
    }
    @Test void alertsOnSustainedExcursion() {
        AlertProperties p = new AlertProperties();
        Map<String,Object> out = service(p).analyze(List.of(r("08:00:00",9),r("08:05:00",9.2),r("08:10:00",9.5),r("08:15:00",9.1)));
        assertEquals(1, ((List<?>)out.get("alerts")).size());
    }
    @Test void outOfOrderReadingsAreSortedBeforeEpisodeEvaluation() {
        AlertProperties p = new AlertProperties();
        Map<String,Object> out = service(p).analyze(List.of(r("08:15:00",9),r("08:00:00",9),r("08:10:00",9),r("08:05:00",9)));
        assertEquals(1, ((List<?>)out.get("alerts")).size());
    }
    @Test void longGapBreaksEpisode() {
        AlertProperties p = new AlertProperties(); p.setMaxGapMinutes(6);
        Map<String,Object> out = service(p).analyze(List.of(r("08:00:00",9),r("08:05:00",9),r("08:20:00",9),r("08:25:00",9)));
        assertEquals(0, ((List<?>)out.get("alerts")).size());
    }
}
