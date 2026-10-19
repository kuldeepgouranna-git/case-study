# ReeferGuard Dynamic — Local Run Guide

A Java 21 + Spring Boot demo for the Nimbus Freight cold-chain alerting case study. It includes batch analysis, configurable rules, a replay mode, a responsive dashboard, REST endpoints, and Spring Boot Actuator.

## Requirements

- JDK 21 (verify with `java -version`)
- Maven 3.9+ (verify with `mvn -version`)
- Browser: Chrome, Edge, Firefox, or similar
- Runs comfortably on a typical 16 GB RAM laptop; no database or Docker required

## Run

From the project root:

```bash
mvn clean test
mvn spring-boot:run
```

Open `http://localhost:8080`.

If Maven is not installed, install Maven 3.9+ and ensure `mvn` is on PATH. In IntelliJ IDEA, import the project as a Maven project, set Project SDK to Java 21, then run `ReeferGuardApplication`.

## Try the demo

1. Open the dashboard at `http://localhost:8080`.
2. Click **Analyze full dataset** to inspect all readings at once.
3. Click **Start replay** to process the supplied records incrementally; pause or reset the replay as needed.
4. Change the threshold, minimum duration, or maximum gap under **Detection policy**, then apply the policy and analyze again.
5. Inspect the incident evidence: truck, start/end time, duration, peak temperature, number of violating readings, and explanation.
6. Check `http://localhost:8080/actuator/health` for the Spring Boot health endpoint.

## API examples

```bash
curl http://localhost:8080/api/analysis
curl http://localhost:8080/api/config
curl -X POST http://localhost:8080/api/simulation/start
curl http://localhost:8080/api/simulation
curl -X POST http://localhost:8080/api/simulation/stop
curl -X POST http://localhost:8080/api/simulation/reset
curl -X PUT "http://localhost:8080/api/config?thresholdC=8.0&minDurationMinutes=15&maxGapMinutes=6"
curl http://localhost:8080/actuator/health
```

## Default policy

| Setting | Default |
|---|---:|
| Temperature threshold | 8°C (strictly greater than) |
| Minimum sustained duration | 15 minutes |
| Maximum gap inside an episode | 6 minutes |
| High-severity peak | 12°C or higher |

These are demo assumptions only. Confirm actual temperature limits and exposure durations with the customer and cargo-specific SOPs before operational use.

## Dataset notes

`data/sensor_readings.json` is the supplied synthetic dataset (five trucks). It contains out-of-order timestamps and a deliberate longer data gap. The analysis sorts each truck's readings before evaluating episodes. Replay preserves the dataset's original order so the UI can demonstrate incremental processing of imperfect input.

## Project structure

```text
src/main/java/com/nimbus/reeferguard/
  ReeferGuardApplication.java
  config/AlertProperties.java
  model/SensorReading.java
  model/AlertEpisode.java
  service/SensorDataService.java
  service/AlertAnalysisService.java
  service/SimulationService.java
  web/AlertController.java
src/main/resources/
  application.yml
  static/index.html
data/sensor_readings.json
src/test/java/com/nimbus/reeferguard/AlertAnalysisServiceTest.java
SOLUTION.md
```

## Current scope and limitations

This is a local demo, not a production streaming system. It uses in-memory analysis, does not persist incidents, and does not send external notifications. Replay re-analyzes the processed prefix for clarity; production should use incremental per-truck state, persistent incident IDs, deduplication, recovery transitions, and an independent stale-sensor alert. Policy changes last until restart.
