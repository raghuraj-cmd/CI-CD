# EXPERIMENT 3.2.1: Continuous Integration (CI) Pipeline with Multi-Level Automated Testing

---

## 1. Aim
To design, implement, and configure an automated **Continuous Integration (CI) Pipeline** that validates building, packaging, and multi-tier testing—encompassing **Unit Testing (JUnit 5 + Mockito)**, **Integration Testing (MockMvc + Spring Data JPA)**, **Contract Testing (Pact)**, and **Infrastructure Testing (Testcontainers for PostgreSQL & Apache Kafka)**—to establish early defect detection, strict quality gates, and automated feedback loops.

---

## 2. Objectives
- **Understand CI Principles**: Comprehend continuous integration workflows, trunk-based development, build automation, and automated validation gates.
- **Implement Unit Testing**: Develop comprehensive, isolated unit tests using **JUnit 5**, **Mockito**, and **AssertJ** covering business logic and boundary conditions.
- **Implement Integration Testing**: Verify end-to-end REST API communication, request parsing, HTTP status semantics, and JPA transactions via **MockMvc**.
- **Perform Consumer-Driven Contract Testing**: Enforce API contract compatibility between microservices (Consumer vs Provider) using **Pact** to avoid breaking changes.
- **Incorporate Infrastructure Testing with Testcontainers**: Utilize lightweight, disposable Docker containers to run tests against real instances of **PostgreSQL** and **Apache Kafka** rather than relying solely on in-memory mocks.
- **Automate CI Workflow via GitHub Actions**: Configure a multi-stage GitHub Actions pipeline (`.github/workflows/ci-pipeline.yml`) that executes compile, unit tests, integration tests, contract tests, and packaging on every push/pull request.

### Course Outcomes (CO) Mapped:
- **CO6 - BT6**: Design and create automated deployment and testing pipelines, demonstrating mastery of continuous integration, automated quality assurance, and containerized test infrastructure.

---

## 3. Pre-requisites & Software Requirements
- **Java Development Kit (JDK)**: OpenJDK 17 or 21 LTS
- **Build Tool**: Apache Maven 3.9+ / Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Testing Frameworks**:
  - JUnit 5 Jupiter
  - Mockito 5.x
  - Spring Boot Test / MockMvc
  - Pact JVM (Consumer JUnit 5 & Provider Spring 4.6.x)
  - Testcontainers 1.19.x (PostgreSQL & Kafka modules)
- **CI Platform**: GitHub Actions
- **Container Runtime**: Docker Desktop (optional for local Testcontainers; built-in on GitHub Actions runners)

---

## 4. Theory & Architecture

Continuous Integration (CI) is a foundational DevOps engineering practice requiring developers to integrate code into a shared repository frequently. Each submission triggers an automated build and test pipeline that flags syntax errors, regression defects, contract violations, and performance issues before merging.

### Test Pyramid Hierarchy in this Project:

```
                      / \
                     /   \
                    /     \
                   / Contract\     <-- Pact (Consumer/Provider Verification)
                  /   Tests   \
                 /-------------\
                /  Integration  \   <-- MockMvc & Testcontainers (Postgres, Kafka)
               /     Tests       \
              /-------------------\
             /     Unit Tests      \  <-- JUnit 5 + Mockito (Business Logic)
            /-----------------------\
```

1. **Unit Tests (JUnit 5 + Mockito)**:
   - Target: `ProductService`
   - Characteristics: Ultra-fast execution (~100ms), zero external dependencies, isolated mocks for repositories.
   - Verifies: CRUD operations, discount percentage bounds, inventory calculations, low-stock threshold queries.

2. **Integration Tests (MockMvc + Spring Context)**:
   - Target: `ProductController` & `ProductRepository`
   - Characteristics: Loads full Spring context and in-memory H2 database.
   - Verifies: HTTP request/response serializations, status codes (`200 OK`, `201 CREATED`, `400 BAD_REQUEST`, `404 NOT_FOUND`, `409 CONFLICT`), and JSON path field assertions.

3. **Contract Testing (Pact)**:
   - Target: Cross-service HTTP boundaries.
   - Characteristics: Consumer defines expectations (`ProductConsumerContractTest`), emitting a pact file (`frontend-client-product-api.json`). The Provider (`ProductProviderContractTest`) boots up and verifies its actual responses match this contract.
   - Benefit: Prevents distributed microservice breaking changes without running the entire microservice ecosystem concurrently.

4. **Infrastructure Testing (Testcontainers)**:
   - Target: `PostgresTestcontainersTest` & `KafkaTestcontainersTest`
   - Characteristics: Programmatically boots genuine Docker containers during integration test runs.
   - Benefit: Eliminates "works on H2, fails on Postgres" production discrepancy.

---

## 5. Directory Structure

```
3.2.1/
├── .github/
│   └── workflows/
│       └── ci-pipeline.yml             # GitHub Actions multi-stage CI pipeline
├── .mvn/
│   └── wrapper/                        # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/experiment/ci/
│   │   │   ├── CiPipelineDemoApplication.java # Spring Boot main entrypoint
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java     # REST API controller endpoints
│   │   │   ├── model/
│   │   │   │   └── Product.java               # JPA Entity with validation
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java     # Spring Data JPA Repository
│   │   │   └── service/
│   │   │       └── ProductService.java        # Core business service logic
│   │   └── resources/
│   │       └── application.properties         # Database and Kafka configuration
│   └── test/
│       └── java/com/experiment/ci/
│           ├── unit/
│           │   └── ProductServiceUnitTest.java          # 24 Unit tests (JUnit 5 + Mockito)
│           ├── integration/
│           │   └── ProductControllerIntegrationTest.java# 11 MockMvc Integration tests
│           ├── contract/
│           │   ├── ProductConsumerContractTest.java     # Pact Consumer contract definition
│           │   └── ProductProviderContractTest.java     # Pact Provider verification against contract
│           └── container/
│               ├── PostgresTestcontainersTest.java      # PostgreSQL Testcontainer test
│               └── KafkaTestcontainersTest.java         # Kafka Testcontainer test
├── mvnw                                # Unix Maven wrapper executable
├── mvnw.cmd                            # Windows Maven wrapper executable
├── pom.xml                             # Maven POM with dependencies & plugins
└── README.md                           # Documentation and lab manual
```

---

## 6. How to Run the Tests

### 1. Compile the Project:
```bash
./mvnw clean test-compile
# Windows:
.\mvnw.cmd clean test-compile
```

### 2. Run Isolated Unit Tests:
```bash
.\mvnw.cmd test -Dtest=ProductServiceUnitTest
```
*Executes 24 unit tests covering CRUD, discount logic, validations, and inventory calculations.*

### 3. Run MockMvc Integration Tests:
```bash
.\mvnw.cmd test -Dtest=ProductControllerIntegrationTest
```
*Executes 11 HTTP integration tests validating request parsing, HTTP status codes, and JSON serialization.*

### 4. Run Contract Tests (Pact):
```bash
# Step 4a: Run Consumer test to generate the contract pact file:
.\mvnw.cmd test -Dtest=ProductConsumerContractTest

# Step 4b: Verify Provider against the generated pact:
.\mvnw.cmd test -Dtest=ProductProviderContractTest
```
*Generated contract location: `target/pacts/frontend-client-product-api.json`*

### 5. Run Testcontainers Tests (Requires Docker):
```bash
.\mvnw.cmd test -Dtest=*TestcontainersTest
```

### 6. Run Complete Test Suite & Package JAR:
```bash
.\mvnw.cmd package
```

---

## 7. GitHub Actions CI Pipeline Execution Flow

The CI pipeline defined in `.github/workflows/ci-pipeline.yml` implements automated gates:

```
[ Git Push / PR ]
       │
       ▼
┌───────────────────────────────┐
│ Job 1: Build & Compile        │  --> Validates Java 17 compile & dependencies
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│ Job 2: Unit Testing           │  --> Runs JUnit 5 & Mockito test suite
└──────────────┬────────────────┘
               │
         ┌─────┴────────────────┐
         ▼                      ▼
┌──────────────────────┐ ┌───────────────────────────┐
│ Job 3: Integration   │ │ Job 4: Contract Testing   │
│ Tests (MockMvc &     │ │ (Pact Consumer -> Pact    │
│ Testcontainers)      │ │ Provider Verification)    │
└──────────────┬───────┘ └──────────────┬────────────┘
               └──────────────┬─────────┘
                              │
                              ▼
               ┌───────────────────────────────┐
               │ Job 5: Package & CI Gate      │  --> Assembles production JAR
               │ Verification                  │  --> Uploads artifacts
               └───────────────────────────────┘
```

---

## 8. Verification & Output Summary

### Unit Tests Output (`ProductServiceUnitTest`):
```text
[INFO] Running com.experiment.ci.unit.ProductServiceUnitTest$QueryTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.experiment.ci.unit.ProductServiceUnitTest$BusinessLogicTests
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.experiment.ci.unit.ProductServiceUnitTest$CrudTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results: Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Integration Tests Output (`ProductControllerIntegrationTest`):
```text
[INFO] Running com.experiment.ci.integration.ProductControllerIntegrationTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results: Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Pact Contract Verification Output (`ProductProviderContractTest`):
```text
Verifying a pact between frontend-client and product-api
  [Using File target\pacts\frontend-client-product-api.json]
  Given a product with id 1 exists
  a GET request for product with id 1
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  Given product creation is available
  a POST request to create a new product
    returns a response which
      has status code 201 (OK)
      has a matching body (OK)
  Given products exist in the system
  a GET request for all products
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
  Given service is running
  a GET request for health status
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 9. Conclusion
Experiment 3.2.1 successfully implements a multi-tiered Continuous Integration pipeline combining unit, integration, contract, and infrastructure testing with GitHub Actions workflow automation, fulfilling all criteria for automated build verification and early defect detection.
