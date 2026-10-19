package com.nimbus.reeferguard.model;

import java.time.LocalDateTime;

public record AlertEpisode(String id, String truckId, LocalDateTime startTime, LocalDateTime endTime,
                           long durationMinutes, int violatingReadings, double peakTemperatureC,
                           String severity, String status, String reason) {}
