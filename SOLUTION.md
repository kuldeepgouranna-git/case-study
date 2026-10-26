# Nimbus Freight--- Cold-Chain Alerting Engine

## 1. Problem and Goal

Nimbus Freight transports temperature-sensitive cargo for pharmaceutical and grocery  in refrigerated trucks. If a truck stays too warm for too long, the operations team
needs to know. Too many alerts create noise, while too few can hide genuine problems.

The app, ignores short temperature spikes, flags sustained temperature excursions, and makes missing sensor data visible.

The current version is an app that uses the supplied static data. It supports full-dataset analysis and a replay mode that shows results progressively in the browser.

## 2. Business Rules and Assumptions

These values are defaults, we have set as default temperature as 8°C regulatory or cargo-specific safety, and 6 min gap duration for guidance. 
Actual limits will be configured specific to pharmaceutical and grocery needs and will be mapped with operating procedures.

  ------------------------------------------------------------------------
  Rule                                  Demo default Why it matters
  --------------------- ---------------------------- ---------------------
  Temperature limit                      Above `8°C` Starting point that
                                                     can be changed

  Minimum time above                      15 minutes Filters brief spikes
  limit                                              

  Maximum gap within an                    6 minutes Avoids assuming
  episode                                            conditions stayed the
                                                     same during long data
                                                     gaps

  High severity           Peak temperature `>= 12°C` Helps prioritize
                                                     attention

  Incident recovery         Not tracked persistently The MVP analyzes data
                                                     snapshots rather than
                                                     managing a full
                                                     incident lifecycle
  ------------------------------------------------------------------------

A temperature episode belongs to one truck. It qualifies as an alert when the first-to-last reading duration meets the minimum time. A normal
reading or a gap above the allowed limit ends the episode. Readings are sorted by timestamp before analysis so out-of-order input does not
distort the batch result.

**Important:** Current app does not cover missing readings of the cargo. The MVP counts long gaps and displays them in the truck summary. 
A production version should also raise a separate sensor-data warning when readings stop arriving, as this needs to notified to cargo members and Ops team.

## 3. High-Level Design

``` mermaid
flowchart TD
    A["Sensor readings JSON"] --> B["SensorDataService"]
    B --> C["Parse readings"]
    C --> D["AlertAnalysisService"]
    D --> E["Group by truck and sort by time"]
    E --> F["Check temperature, duration and gaps"]
    F --> G{"Alert qualifies?"}
    G -->|No| H["Suppress short episode"]
    G -->|Yes| I["Build alert evidence"]
    H --> J["Truck summaries and gap counts"]
    I --> J
    J --> K["REST API"]
    K --> L["Browser dashboard"]
    M["SimulationService"] -->|Replay readings| D
```

### Main Components

-   **Spring Boot:** Runs the application and connects its components.
-   **`SensorDataService`:** Reads the static data and converts records into Java objects.
-   **`AlertAnalysisService`:** Groups readings by truck, checks the
    rules, and builds alert evidence.
-   **`AlertProperties`:** Stores configurable limits, durations, gaps,
    and replay speed.
-   **`SimulationService`:** Replays readings progressively for the
    demo. It re-analyzes the processed data and is not designed for
    high-volume streaming.
-   **`AlertController`:** Provides API endpoints for analysis, replay,
    and settings.
-   **Dashboard:** Shows key figures, alerts, truck summaries, settings,
    and replay progress.
-   **Actuator:** Provides application health and metrics endpoints.

## 4. Main Users and Use Cases

``` mermaid
flowchart LR
    Ops[Operations user]
    Demo[Demo operator]
    Analyze[Analyze readings]
    Review[Review alert evidence]
    Replay[Start, pause or reset replay]
    Policy[Change detection settings]
    Health[Check application health]
    Ops --> Analyze
    Ops --> Review
    Ops --> Replay
    Demo --> Policy
    Demo --> Health
    Policy -. changes rules .-> Analyze
    Replay -. shows results over time .-> Review
```

## 5. How Alert Detection Works

1.  Read the sensor records from the static data.
2.  Group records by truck and sort them by timestamp.
3.  Mark readings above the configured temperature limit as violations.
4.  Group nearby violating readings into episodes. A normal reading or a long data gap ends an episode.
5.  Calculate the episode duration using timestamps, not just the number of readings.
6.  Suppress episodes that do not meet the minimum duration.
7.  For qualifying episodes, return the truck, time window, duration,
    number of violating readings, peak temperature, severity, and
    reason.
8.  Return the results to the dashboard through the REST API.

## 6. REST API

  ------------------------------------------------------------------------------------------------------------------
  Method                  Endpoint                                                           Purpose
  ----------------------- ------------------------------------------------------------------ -----------------------
  `GET`                   `/api/analysis`                                                    Analyze the complete
                                                                                             dataset

  `GET`                   `/api/config`                                                      View current detection
                                                                                             settings

  `PUT`                   `/api/config?thresholdC=8&minDurationMinutes=15&maxGapMinutes=6`   Update and validate
                                                                                             detection settings

  `GET`                   `/api/simulation`                                                  View replay status and
                                                                                             latest results

  `POST`                  `/api/simulation/start`                                            Start replay

  `POST`                  `/api/simulation/stop`                                             Pause replay

  `POST`                  `/api/simulation/reset`                                            Return replay to the
                                                                                             beginning

  `GET`                   `/api/health`                                                      Simple application
                                                                                             status

  `GET`                   `/actuator/health`                                                 Spring Boot health
                                                                                             status

  `GET`                   `/actuator/metrics`                                                Available application
                                                                                             metrics
  ------------------------------------------------------------------------------------------------------------------

## 7. Key Design Decisions

-   **Separate trucks:** Readings from different trucks are never combined.
-   **Correct time order:** Readings are sorted before duration is calculated.
-   **Real elapsed time:** Episode duration comes from timestamps.
-   **Careful handling of gaps:** Long gaps break an episode instead of assuming the temperature stayed high or returned to normal.
-   **Explainable alerts:** Each alert includes the measurements and rules that caused it to qualify.
-   **Changeable settings:** Operators can change demo rules without rebuilding the application.
-   **No real notifications:** The local demo does not send email or SMS messages.

## 8. Current Limitations

1.  Readings and incidents are held in memory and are not saved across restarts.
2.  Replay re-analyzes the processed readings and is intended for demonstration, not large-scale ingestion.
3.  Incident IDs may change during re-analysis; production needs stable incident identity and lifecycle tracking.
4.  Recovery tracking, escalation timers, acknowledgements, and notification retries are not implemented.
5.  Long data gaps are counted but do not yet create separate sensor-health alerts.
6.  Settings changed through the demo reset to `application.yml` after restart.
7.  The application does not yet include a database, message broker, authentication, or external notification service.

## 9. Future Enhancements

The enhancements below are a suggested roadmap. The order should be
confirmed against customer needs and operational risks.

  -----------------------------------------------------------------------
  Priority                Enhancement             Business benefit
  ----------------------- ----------------------- -----------------------
  P0 --- Foundation       Save readings and       Keeps history available
                          incidents in PostgreSQL 

  P0 --- Foundation       Track incident states   Prevents repeated
                          from normal to active   alerts for the same
                          to recovered            incident and supports
                                                  recovery tracking

  P0 --- Foundation       Raise separate alerts   Helps teams identify
                          when sensor data is     monitoring blind spots
                          missing or unreliable   

  P1 --- Reliability      Add notification        Improves confidence
                          retries, duplicate      that important alerts
                          protection, and         are received and
                          acknowledgements        handled

  P1 --- Operations       Add AI-generated alert  Helps operators
                          explanations            understand an alert
                                                  quickly

  P1 --- Access and audit Add user access         Improves accountability
                          controls and a record   and traceability
                          of policy changes       

  P2 --- Policy           Support cargo- and      Aligns monitoring with
                          customer-specific rules agreed operating
                                                  procedures

  P2 --- Scale            Add a message broker or Supports higher data
                          streaming platform if   volumes when justified
                          required                

  P2 --- Reporting        Show temperature        Helps teams spot
                          trends, recurring       recurring operational
                          incidents, and sensor   problems
                          quality                 

  P3 --- Predictive       Explore anomaly         May help identify
  insights                detection and           developing equipment
                          predictive maintenance  issues earlier
						  
						  
  -----------------------------------------------------------------------

### 9.1 AI-Powered Alert Explanation for Future enhancement

1.  We can add AI service as Java + Spring Boot + Spring AI	 for Prompt construction, model integration, and validation, 
	even Spring AI supports AI-operation metrics and tracing through the Spring ecosystem.
2.  We can extend to LLM inference(layer) Managed enterprise model API or privately hosted model endpoint which can give clarity over 
	Balance quality, latency, and operating cost.
3.  Will assit in Generate and validate the explanation via the AI service calls the configured model, validates the output, 
	and stores the result against the alert ID and model/prompt versions.
4.  The UI displays the original alert immediately and adds the AI summary when available. If AI fails, the alert remains fully usable.
5.  To avoid duplicate event delivery AI worker should use an idempotency key based on the incident ID, incident version and explanation type. 
	This avoids unnecessary repeated model calls and duplicate saved summaries.