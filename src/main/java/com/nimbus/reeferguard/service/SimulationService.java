package com.nimbus.reeferguard.service;

import com.nimbus.reeferguard.config.AlertProperties;
import com.nimbus.reeferguard.model.SensorReading;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * Replays sensor readings incrementally and analyzes the readings as they arrive.
 * The simulation runs on a scheduled background thread and exposes its current state.
 */
@Service
public class SimulationService {
    private final SensorDataService data;
    private final AlertAnalysisService analyzer;
    private final AlertProperties properties;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sensor-simulation");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> task;
    private volatile List<SensorReading> readings = new ArrayList<>();
    private volatile int cursor = 0;
    private volatile boolean running = false;
    private volatile Map<String, Object> latest = Map.of("status", "IDLE", "processedReadings", 0);

    /**
     * Creates the simulation service and loads the readings available for replay.
     *
     * @param data service used to load sensor readings
     * @param analyzer service used to analyze the readings
     * @param properties configuration containing the simulation delay
     */
    public SimulationService(SensorDataService data, AlertAnalysisService analyzer, AlertProperties properties) {
        this.data = data;
        this.analyzer = analyzer;
        this.properties = properties;
        reset();
    }

    /**
     * Starts replaying the loaded readings, resetting the replay when it has
     * already completed.
     *
     * @return the current simulation status
     */
    public synchronized Map<String, Object> start() {
        if (running)
            return status();
        if (cursor >= readings.size())
            reset();
        running = true;
        task = executor.scheduleAtFixedRate(this::tick, 0, Math.max(150, properties.getSimulationDelayMs()),
                TimeUnit.MILLISECONDS);
        return status();
    }

    /**
     * Stops the replay if it is running.
     *
     * @return the simulation status after stopping
     */
    public synchronized Map<String, Object> stop() {
        running = false;
        if (task != null)
            task.cancel(false);
        latest = status();
        return latest;
    }

    /**
     * Stops the replay and reloads the sensor readings from the data service.
     *
     * @return the initial status for the reloaded simulation
     */
    public synchronized Map<String, Object> reset() {
        if (task != null)
            task.cancel(false);
        running = false;
        readings = new ArrayList<>(data.loadReadings());
        cursor = 0;
        latest = Map.of("status", "IDLE", "processedReadings", 0, "totalSimulationReadings", readings.size(), "message",
                "Ready to replay dataset");
        return latest;
    }

    /**
     * Processes the next reading batch or marks the replay complete when all
     * readings have been processed.
     */
    private synchronized void tick() {
        if (!running)
            return;
        if (cursor >= readings.size()) {
            running = false;
            if (task != null)
                task.cancel(false);
            latest = status();
            return;
        }
        // Simulate incremental arrivals while preserving original input order to expose
        // out-of-order data.
        cursor++;
        Map<String, Object> result = analyzer.analyze(readings.subList(0, Math.min(cursor, readings.size())));
        Map<String, Object> state = new LinkedHashMap<>(result);
        state.put("status", "RUNNING");
        state.put("processedReadings", cursor);
        state.put("totalSimulationReadings", readings.size());
        latest = state;
    }

    /**
     * Returns a snapshot of the simulation state, including its effective status
     * and reading counts.
     *
     * @return a map describing the current simulation status
     */
    public Map<String, Object> status() {
        Map<String, Object> s = new LinkedHashMap<>(latest);
        s.put("status",
                running ? "RUNNING"
                        : (cursor >= readings.size() && cursor > 0 ? "COMPLETED"
                                : String.valueOf(s.getOrDefault("status", "IDLE"))));
        s.put("processedReadings", cursor);
        s.put("totalSimulationReadings", readings.size());
        return s;
    }

    /**
     * Returns the current simulation state.
     *
     * @return a map describing the current simulation status
     */
    public Map<String, Object> current() {
        return status();
    }
}
