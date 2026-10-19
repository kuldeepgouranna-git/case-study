package com.nimbus.reeferguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reefer.alert")
public class AlertProperties {
    private double thresholdC = 8.0;
    private int minDurationMinutes = 15;
    private int maxGapMinutes = 6;
    private double highSeverityC = 12.0;
    private int simulationDelayMs = 700;
    public double getThresholdC() { return thresholdC; }
    public void setThresholdC(double v) { thresholdC = v; }
    public int getMinDurationMinutes() { return minDurationMinutes; }
    public void setMinDurationMinutes(int v) { minDurationMinutes = v; }
    public int getMaxGapMinutes() { return maxGapMinutes; }
    public void setMaxGapMinutes(int v) { maxGapMinutes = v; }
    public double getHighSeverityC() { return highSeverityC; }
    public void setHighSeverityC(double v) { highSeverityC = v; }
    public int getSimulationDelayMs() { return simulationDelayMs; }
    public void setSimulationDelayMs(int v) { simulationDelayMs = v; }
}
