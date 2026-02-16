# 🌟 Portfolio Project Entry: Banking Microservices System

## 📸 Project Snapshot
**Name:** Banking Microservices Platform
**Role:** Backend / Cloud Engineer
**Tech Stack:** Java 21, Spring Boot, Spring Cloud, Docker, Kubernetes, Kafka, Grafana Stack (LGTM), MySQL, Keycloak.

## 📝 Description
A robust, cloud-native banking solution designed to handle core financial operations through a distributed microservices architecture. The system decomposes complex banking domains into scalable, independent services (Accounts, Loans, Cards) capable of handling high concurrency and ensuring data consistency.

## 💡 Key Skills & Implementations

### 🏗️ Microservices Architecture
*   **Service Decomposition**: Designed loosely coupled services based on Domain-Driven Design (DDD) principles.
*   **Service Discovery**: Implemented client-side discovery using **Netflix Eureka** integration.
*   **API Gateway**: Configured **Spring Cloud Gateway** to handle cross-cutting concerns (routing, load balancing, security implementation).
*   **Centralized Configuration**: Managed environment-specific configurations securely using **Spring Cloud Config**.

### ⚡ Event-Driven & Async Communication
*   **Apache Kafka**: Architected an event-driven system where services publish and consume events (e.g., account creation, transaction alerts) using **Spring Cloud Stream**, decoupling critical paths and improving system responsiveness.

### 🛡️ Security & Reliability
*   **OAuth2 / OIDC**: Implemented a secure authentication flow using **Keycloak** as the Identity Provider (IdP) and Resource Server.
*   **Fault Tolerance**: Applied **Resilience4j** patterns (Circuit Breaker, Time Limiter, Retry) to prevent cascading failures and ensure system uptime during partial outages.

### 📊 Observability & DevOps
*   **Distributed Tracing**: Integrated **OpenTelemetry** and **Grafana Tempo** to trace requests across microservice boundaries, reducing Mean Time To Resolution (MTTR).
*   **Monitoring**: Set up **Prometheus** and **Grafana** dashboards to monitor JVM metrics, API latency, and system health.
*   **Log Aggregation**: Centralized logs using **Grafana Loki** for efficient troubleshooting.
*   **Container orchestration**: Containerized all applications using **Docker** (via Jib) and deployed to **Kubernetes** clusters using custom **Helm Charts**.

## 🚀 Challenges Overcome
*   **Distributed Transactions**: Solved data consistency challenges in a distributed environment by adopting event consistency patterns.
*   **Configuration Drift**: Eliminated configuration discrepancies between environments (Dev/QA/Prod) by enforcing a strict GitOps workflow with Config Server.
*   **Visibility**: Transformed a "black box" distributed system into a transparent one by implementing the full LGTM observability stack.
