# Performance Tests CI: JMeter, Gatling, Lighthouse on Jenkins

Jenkins pipeline that runs performance tests against the **Performance Testing Essentials** training application (WordPress shop) and publishes HTML reports as Jenkins artifacts.

| Test | Type | Tool | Load model |
|---|---|---|---|
| JMeter | Backend | Apache JMeter 5.6.3 | 3–5 concurrent users, 10 minutes |
| Gatling | Backend | Gatling 3.13 (Maven wrapper) | 3–5 concurrent users, 10 minutes |
| Lighthouse | UI | Lighthouse 13 user flow + Puppeteer | 3–10 loops of the script |

## Architecture

```
GitHub (perf-tests repo) ──► Jenkins controller ──SSH──► Agent "perf-agent" (label: perf)
                             0 executors                 JMeter, Java, Node.js, Chrome
                             (no builds run here)        runs all tests ──► WordPress app
```

- Tests and the `Jenkinsfile` are stored in Git; Jenkins uses **Pipeline script from SCM**.
- The controller (Built-In Node) has **0 executors**, so every stage runs on the agent `perf-agent` (`agent { label 'perf' }`).
- Platform: Linux (Ubuntu 22.04, WSL2).

## Repository structure

```
perf-tests/
├── Jenkinsfile                  # pipeline
├── jmeter/
│   ├── test-plan.jmx            # parameterized: threads, rampup, duration, host, port
│   └── csv/order-data.csv       # test data (path relative to .jmx)
├── gatling/
│   ├── pom.xml, mvnw, .mvn/     # Maven project, wrapper downloads Maven itself
│   └── src/test/java/...        # simulations.PerfTestSimulation (parameterized)
└── lighthouse/
    ├── lighthouse-flow.js       # user flow: Home → Tables → Product → Add to cart → Cart → Checkout → Place order
    ├── package.json
    └── package-lock.json
```

## Prerequisites

### Application
Performance Testing Essentials app deployed and reachable from the agent (default `http://localhost:80`).

### Agent (Linux)
| Tool | Version used | Install / location |
|---|---|---|
| Java (OpenJDK) | 21 | `sudo apt install openjdk-21-jdk` |
| Git | 2.34 | `sudo apt install git` |
| Apache JMeter | 5.6.3 | unpacked to `/opt/jmeter` |
| Maven | via `./mvnw` | no global install needed |
| Node.js / npm | 20+ (22.19+ recommended for Lighthouse 13) | NodeSource repo |
| Google Chrome | stable | `google-chrome-stable_current_amd64.deb` |
| OpenSSH server | — | `sudo apt install openssh-server` (agent is launched via SSH) |

Lighthouse and Puppeteer are installed per build by `npm ci` from `lighthouse/package-lock.json`.

### Jenkins plugins
- **Pipeline** (workflow-aggregator)
- **Git**
- **HTML Publisher**
- **SSH Build Agents**
- **Credentials** / **Credentials Binding** (bundled)

## Jenkins setup

1. **Credentials**: *Manage Jenkins → Credentials → Global → Add*: `Username with password`, ID `perf-agent-ssh` (agent OS user).
2. **Agent node**: *Manage Jenkins → Nodes → New Node* `perf-agent` (Permanent Agent):
   - Executors: `1`, Remote root: `/home/<user>/jenkins-agent`, Label: `perf`
   - Usage: *Only build jobs with label expressions matching this node*
   - Launch: *Launch agents via SSH*, host of the agent, credentials `perf-agent-ssh`
3. **Controller**: *Nodes → Built-In Node → Configure* → Number of executors `0`.
4. **Allow JS/CSS in HTML reports** (otherwise JMeter/Gatling/Lighthouse reports render without charts). Permanent, via systemd override:
```bash
   sudo mkdir -p /etc/systemd/system/jenkins.service.d
   printf '[Service]\nEnvironment="JAVA_OPTS=-Djava.awt.headless=true -Dhudson.model.DirectoryBrowserSupport.CSP="\n' \
     | sudo tee /etc/systemd/system/jenkins.service.d/override.conf
   sudo systemctl daemon-reload && sudo systemctl restart jenkins
```
5. **Job**: *New Item* → `perf-tests-pipeline` → **Pipeline**:
   - Definition: *Pipeline script from SCM*, SCM: Git
   - Repository URL: `https://github.com/oksanamartyniuk1007-commits/perf-tests.git`
   - Branch: `*/main`, Script Path: `Jenkinsfile`

## Pipeline usage

Open the job → **Build with Parameters**.

| Parameter | Default | Description |
|---|---|---|
| `RUN_JMETER` | ✔ | Run JMeter backend test |
| `RUN_GATLING` | ✔ | Run Gatling backend test |
| `RUN_LIGHTHOUSE` | ✔ | Run Lighthouse UI test |
| `APP_HOST` | `localhost` | Application host as seen from the agent |
| `APP_PORT` | `80` | Application port |
| `USERS` | `3` | Backend concurrent users (3 / 4 / 5) |
| `DURATION_SEC` | `600` | Backend total duration incl. ramp-up (600 s = 10 min) |
| `RAMPUP_SEC` | `60` | Backend ramp-up |
| `LH_LOOPS` | `3` | Lighthouse script loops (3–10) |

Any combination of tests can be selected; at least one is required.

### Stages
1. **Validate & Agent info**: checks selection, prints agent, tool versions, application HTTP status.
2. **JMeter**: `jmeter -n` with `-Jthreads/-Jrampup/-Jduration/-Jhost/-Jport`, generates HTML dashboard.
3. **Gatling**: `./mvnw gatling:test` with `-DUSER_COUNT/-DRAMP_UP_PERIOD/-DTEST_DURATION/-DBASE_URL`.
4. **Lighthouse**: `npm ci`, then runs `lighthouse-flow.js` `LH_LOOPS` times (headless Chrome), one HTML report per loop plus an index page.

Each test stage is wrapped in `catchError`, so a failure in one tool does not skip the others.

### Reports
On the build page (left menu):
- **JMeter Report**: JMeter HTML dashboard
- **Gatling Report**: Gatling HTML report
- **Lighthouse Report**: index with links to `run-1.html … run-N.html`

All reports (plus the raw `results.jtl` and `jmeter.log`) are also archived as **Build Artifacts** (`reports/**`).

## Running scripts manually on the agent

```bash
# JMeter
/opt/jmeter/bin/jmeter -n -t jmeter/test-plan.jmx -Jthreads=3 -Jrampup=60 -Jduration=600 \
  -Jhost=localhost -Jport=80 -l results.jtl -e -o jmeter-report

# Gatling
cd gatling && ./mvnw gatling:test -Dgatling.simulationClass=simulations.PerfTestSimulation \
  -DUSER_COUNT=3 -DRAMP_UP_PERIOD=60 -DTEST_DURATION=600 -DBASE_URL=http://localhost

# Lighthouse (HEADLESS=false to watch the browser)
cd lighthouse && npm ci && BASE_URL=http://localhost REPORT_NAME=report.html node lighthouse-flow.js
```

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| First build has empty parameters | Jenkins learns parameters after the first run; the Jenkinsfile falls back to defaults, use **Build with Parameters** afterwards |
| `Failed to exec spawn helper ... JDK version mismatch` | Java was updated while Jenkins was running → `sudo systemctl restart jenkins` |
| Reports open without charts | CSP not relaxed → see Jenkins setup, step 4 |
| JMeter: 0 samples | CSV not found → keep `jmeter/csv/order-data.csv` next to the `.jmx` |
