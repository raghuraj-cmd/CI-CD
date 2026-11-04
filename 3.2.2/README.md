# EXPERIMENT 3.2.2: Continuous Deployment (CD) Pipeline with Containerization, Canary Releases, and Automated Rollbacks

---

## 1. Aim
To design, implement, and orchestrate an enterprise-grade **Continuous Deployment (CD) Pipeline** featuring multi-stage Docker containerization, Spring Boot Actuator readiness and liveness probes, automated container registry publishing (GitHub Packages / Docker Hub), and advanced deployment strategies—specifically **Canary Releases with Weighted Traffic Splitting (10% -> 100%)**, automated error-budget monitoring, and self-healing rollback automation.

---

## 2. Objectives
- **Understand Continuous Deployment Concepts**: Distinguish Continuous Delivery (automated artifact staging with manual gates) from Continuous Deployment (fully automated production promotion governed by health probes).
- **Containerize Applications via Multi-Stage Docker**: Create a minimal, hardened, non-root JRE Alpine container image with Spring Boot layertools caching.
- **Implement Canary Deployment Strategy**:
  - Deploy new release versions alongside existing stable instances.
  - Dynamically split production traffic (e.g., 90% Stable v1.0.0, 10% Canary v2.0.0).
  - Support targeted verification routing via HTTP request headers (`X-Canary: always` / `X-Canary: never`).
  - Monitor real-time error rates (< 1% threshold).
  - Automatically promote healthy canaries to 100% production stable, or trigger immediate automated rollback if errors spike.
- **Automate Deployment via GitHub Actions**: Configure `.github/workflows/cd-pipeline.yml` covering 8 sequential stages: CI Test -> Multi-Stage Containerization & Push to Container Registry -> Trivy Security Scanning -> Staging Deploy & Smoke Testing -> Canary Deployment (10%) -> Synthetic Canary Verification -> Full Production Promotion -> Failure-Guarded Rollback.
- **Provide Self-Healing Automation Scripts**: Implement cross-platform bash (`.sh`) and PowerShell (`.ps1`) deployment, smoke-testing, promotion, and rollback automation.

### Course Outcomes (CO) Mapped:
- **CO6 - BT6**: Design and create resilient deployment pipelines, demonstrating containerization best practices, production readiness evaluation, automated health checking, and fault-tolerant continuous deployment strategies.

---

## 3. Pre-requisites & Software Requirements
- **Java Development Kit**: OpenJDK 17 or 21 LTS
- **Build Tool**: Apache Maven 3.9+ / Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Containerization**: Docker 24.x+ & Docker Compose v2.x
- **Framework**: Spring Boot 3.2.x (`spring-boot-starter-web`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa`)
- **Container Registry**: GitHub Packages (`ghcr.io`) / Docker Hub
- **CI/CD Platform**: GitHub Actions
- **Security Scanner**: Aquasecurity Trivy

---

## 4. Theory & Canary Deployment Architecture

### What is Canary Deployment?
A **Canary Deployment** is an advanced release strategy where new application changes are rolled out incrementally to a small subset of users (e.g. 10%) before expanding to 100% of the fleet. By isolating real user traffic to a fraction of the workload while monitoring telemetry and error budgets, any defect is contained without impacting the wider user base.

```
                          ┌────────────────────────┐
                          │   Incoming Requests    │
                          └───────────┬────────────┘
                                      │
                                      ▼
                        ┌────────────────────────────┐
                        │   Canary Traffic Router    │
                        │ (Nginx / App Router Proxy) │
                        └──────┬──────────────┬──────┘
                               │              │
                   90% Traffic │              │ 10% Traffic
                   (or normal) │              │ (or X-Canary: always)
                               ▼              ▼
                    ┌──────────────────┐  ┌──────────────────┐
                    │    app-stable    │  │    app-canary    │
                    │   Version 1.0.0  │  │ Version 2.0.0-can│
                    └────────┬─────────┘  └────────┬─────────┘
                             │                     │
                             └──────────┬──────────┘
                                        ▼
                             ┌─────────────────────┐
                             │ PostgreSQL Database │
                             └─────────────────────┘
```

### Canary Decision Lifecycle:
1. **Initial Canary (10%)**: The new container version is booted alongside stable instances. 10% of traffic is routed to Canary; 90% remains on Stable.
2. **Synthetic Validation**: Probes send requests with header `X-Canary: always` to evaluate Canary latency and HTTP status codes.
3. **Error Budget Analysis**: If Canary error rate exceeds 0%, the pipeline immediately aborts and triggers `canary-rollback.sh`, isolating Canary traffic.
4. **Gradual Expansion**: If healthy, traffic is increased (e.g., 25% -> 50% -> 100%).
5. **Full Promotion**: Stable is updated to the Canary version, and Canary resources are reclaimed.

---

## 5. Directory Structure

```
3.2.2/
├── .github/
│   └── workflows/
│       └── cd-pipeline.yml             # GitHub Actions 8-stage automated CD pipeline
├── .mvn/
│   └── wrapper/                        # Maven wrapper configuration
├── nginx/
│   └── nginx.conf                      # Nginx Canary reverse proxy with weighted & header routing
├── scripts/
│   ├── canary-deploy.sh                # Linux Canary deployment & synthetic test script
│   ├── canary-deploy.ps1               # Windows PowerShell Canary deployment script
│   ├── canary-promote.sh               # Linux Canary promotion script (expands to 100%)
│   ├── canary-promote.ps1              # Windows PowerShell Canary promotion script
│   ├── canary-rollback.sh              # Linux Canary rollback script (isolates canary)
│   ├── canary-rollback.ps1             # Windows PowerShell Canary rollback script
│   ├── smoke-test.sh                   # Linux/CI automated smoke-test script
│   ├── smoke-test.ps1                  # Windows PowerShell smoke-test script
│   ├── rollback.sh                     # Linux/CI general rollback script
│   └── rollback.ps1                    # Windows PowerShell general rollback script
├── src/
│   ├── main/
│   │   ├── java/com/experiment/cd/
│   │   │   ├── CdPipelineDemoApplication.java # Spring Boot application entry point
│   │   │   ├── controller/
│   │   │   │   ├── CanaryController.java      # Canary routing, weights, promotion & simulation endpoints
│   │   │   │   └── DeploymentController.java  # Deployment status & smoke-test endpoints
│   │   │   ├── health/
│   │   │   │   └── DeploymentHealthIndicator.java # Custom Actuator health probe
│   │   │   ├── model/
│   │   │   │   ├── CanaryStatus.java          # Real-time Canary telemetry & status model
│   │   │   │   └── DeploymentRelease.java     # Deployment release tracking model
│   │   │   └── service/
│   │   │       └── CanaryService.java         # Weighted traffic splitting, header routing & state engine
│   │   └── resources/
│   │       ├── application.properties         # Actuator and container configuration
│   │       └── static/
│   │           └── index.html                 # Interactive Canary Release Command Dashboard
│   └── test/
│       ├── java/com/experiment/cd/
│       │   ├── CanaryDeploymentTest.java      # 7 Automated Canary routing, promotion & rollback tests
│       │   └── DeploymentControllerTest.java  # 5 Automated smoke & health verification tests
│       └── resources/
│           └── mockito-extensions/
│               └── org.mockito.plugins.MockMaker # Mockito configuration
├── docker-compose.yml                  # Canary architecture with Stable, Canary, Nginx Router & Postgres
├── Dockerfile                          # Multi-stage production container build
├── mvnw                                # Unix Maven wrapper
├── mvnw.cmd                            # Windows Maven wrapper
├── pom.xml                             # Maven POM with Spring Boot Actuator
└── README.md                           # Documentation and lab manual
```

---

## 6. How to Run and Test

### 1. Build and Run All Automated Tests (12 Tests):
```bash
cd 3.2.2
.\mvnw.cmd clean test
# Unix / macOS:
./mvnw clean test
```
*Executes all 12 automated unit and integration tests covering Canary traffic splitting, header-based routing, fault injection, automated rollback triggers, actuator readiness probes, and Canary promotion.*

### 2. Run the Application Locally:
```bash
.\mvnw.cmd spring-boot:run
```
Open your browser at **http://localhost:8080** to view the interactive **Canary Release & Continuous Deployment Command Dashboard**.

### 3. Build the Multi-Stage Docker Container Image:
```bash
docker build -t cd-pipeline-demo:1.0.0 .
```

### 4. Run Full Containerized Canary Cluster via Docker Compose:
```bash
docker compose up -d
```
*Spins up:*
- `postgres-db`: PostgreSQL 15 database service
- `app-stable`: Production Stable application container (v1.0.0)
- `app-canary`: Canary Release application container (v2.0.0-canary)
- `canary-router`: Nginx reverse proxy balancing 90% Stable and 10% Canary, with instant `X-Canary: always` header routing.

### 5. Execute Canary Deployment Automation Script:
**On Linux / Git Bash:**
```bash
chmod +x scripts/canary-deploy.sh
./scripts/canary-deploy.sh http://localhost:8080 v2.0.0-canary 10
```

**On Windows PowerShell:**
```powershell
.\scripts\canary-deploy.ps1 -TargetHost "http://localhost:8080" -CanaryTag "v2.0.0-canary" -CanaryWeight 10
```

### 6. Promote Canary to 100% Production:
```powershell
.\scripts\canary-promote.ps1 -TargetHost "http://localhost:8080"
```

### 7. Simulate Canary Fault & Observe Automated Rollback:
```powershell
# Injects failure into canary instance:
Invoke-RestMethod -Uri "http://localhost:8080/api/canary/simulate-failure" -Method Post

# Canary rollback is triggered:
.\scripts\canary-rollback.ps1 -TargetHost "http://localhost:8080"
```

---

## 7. Automated CD Pipeline (GitHub Actions 8-Stage Flow)

The automated workflow in `.github/workflows/cd-pipeline.yml` executes the following continuous deployment lifecycle:

```
[ Push to main / Tag v*.*.* ]
              │
              ▼
┌────────────────────────────────────────┐
│ Stage 1: Build & Automated Tests       │  --> Validates 12 unit & integration tests
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 2: Containerize & Push           │  --> Multi-stage Docker build
│          to Container Registry         │  --> Pushes to ghcr.io / Docker Hub
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 3: Security Scan (Trivy)         │  --> Vulnerability gate (CRITICAL/HIGH)
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 4: Deploy to Staging             │  --> Deploys release to staging environment
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 5: Automated Smoke Testing       │  --> Validates /actuator/health & smoke endpoints
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 6: Deploy Canary Release (10%)   │  --> Allocates 10% traffic to Canary
│          & Synthetic Verification      │  --> Runs X-Canary synthetic tests
└──────────────────┬─────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────┐
│ Stage 7: Canary Promotion (100% Prod)  │  --> Expands Canary from 10% to 100%
│          & Final Readiness Check       │  --> Promotes release to Production
└──────────────────┬─────────────────────┘
                   │
                   ▼ (Failure Guard)
┌────────────────────────────────────────┐
│ Stage 8: Automated Rollback on Failure │  --> Triggers canary-rollback.sh
└────────────────────────────────────────┘
```

---

## 8. Test Execution Results

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.experiment.cd.CanaryDeploymentTest
MockHttpServletResponse:
           Status = 200
             Body = {"servedBy":"CANARY","version":"2.0.0-canary","status":"SUCCESS","statusCode":200,"routingReason":"Header-based routing (X-Canary: always)","health":"HEALTHY"}

[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.428 s -- in com.experiment.cd.CanaryDeploymentTest
[INFO] Running com.experiment.cd.DeploymentControllerTest
MockHttpServletResponse:
           Status = 200
             Body = {"environment":"local","deploymentType":"STABLE","uptimeMillis":8267,"service":"cd-pipeline-demo","version":"1.0.0","status":"HEALTHY"}

MockHttpServletResponse:
           Status = 200
             Body = {"environment":"local","status":"READY","smokeTest":"PASSED","version":"1.0.0"}

[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.150 s -- in com.experiment.cd.DeploymentControllerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 9. Conclusion
Experiment 3.2.2 successfully implements an automated Continuous Deployment (CD) pipeline with multi-stage Docker containerization, container registry integration (`ghcr.io` / Docker Hub), Trivy security vulnerability scanning, Nginx reverse proxy orchestration, **Canary Release deployment strategy with dynamic traffic splitting**, automated synthetic verification, and self-healing rollback automation.
