package com.nimbus.reeferguard.model;

import java.time.LocalDateTime;

public record SensorReading(String truckId, LocalDateTime timestamp, double temperatureC, double humidityPct) {}
