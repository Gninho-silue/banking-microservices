# 🏦 Banking Microservices Application

A modern, cloud-native banking application built with Spring Boot microservices, demonstrating a robust architecture for scalable financial systems. This project implements core banking functionalities such as Accounts, Loans, and Cards management, orchestrated through a distributed system.

## 🚀 Key Features

*   **Microservices Architecture**: Decomposed into loosely coupled services (Accounts, Loans, Cards) for independent development and scaling.
*   **Centralized Configuration**: Uses **Spring Cloud Config Server** to manage configurations across all environments (dev, qa, prod).
*   **Service Discovery**: Implemented **Netflix Eureka** for dynamic service registration and discovery.
*   **API Gateway**: **Spring Cloud Gateway** acts as the single entry point, handling routing, cross-cutting concerns, and security.
*   **Event-Driven Communication**: **Apache Kafka** is used for asynchronous communication between microservices (e.g., sending notifications via the `message` service).
*   **Resiliency & Fault Tolerance**: Integration of **Resilience4j** for Circuit Breaker, Retry, and Rate Limiter patterns to ensure system stability.
*   **Observability & Monitoring (LGTM Stack)**:
    *   **Logs**: Grafana Loki
    *   **Metrics**: Prometheus & Grafana
    *   **Tracing**: Grafana Tempo & OpenTelemetry (with Grafana Alloy collector)
*   **Security**: Secured with **Keycloak** (OAuth2 / OIDC) for robust authentication and authorization.
*   **Containerization**: Dockerized services using **Jib** and orchestrated with **Docker Compose**.
*   **Kubernetes Ready**: Includes **Helm Charts** and **K8s manifests** for deployment to Kubernetes clusters.
*   **API Documentation**: Comprehensive API documentation using **SpringDoc OpenAPI (Swagger)**.

## 🛠️ Technology Stack

*   **Core**: Java 21, Spring Boot 3.2+
*   **Spring Cloud**: Config, Gateway, Netflix Eureka, OpenFeign, Stream (Kafka), CircuitBreaker
*   **Database**: MySQL (Per-service database pattern), Spring Data JPA
*   **Messaging**: Apache Kafka, RabbitMQ (optional support)
*   **Security**: Keycloak, Spring Security, OAuth2 Resource Server
*   **Observability**: OpenTelemetry, Grafana, Prometheus, Loki, Tempo
*   **Build & Deploy**: Maven, Docker, Docker Compose, Kubernetes, Helm

## 🏗️ Architecture

The application consists of the following microservices:

1.  **Config Server**: Centralized configuration management.
2.  **Eureka Server**: Service registry and discovery.
3.  **Gateway Server**: Main entry point for all client requests.
4.  **Accounts Service**: Manages customer bank accounts and transactions.
5.  **Loans Service**: Handles loan processing and management.
6.  **Cards Service**: Manages credit and debit card information.
7.  **Message Service**: Consumes events from Kafka to handle notifications (e.g., email/SMS).

## 🚀 Getting Started

### Prerequisites

*   Java 21 SDK
*   Docker & Docker Compose
*   Maven

### Installation & Running

1.  **Clone the repository**
    ```bash
    git clone https://github.com/gninho/banking-microservices.git
    cd banking-microservices
    ```

2.  **Build the project**
    Using the root `pom.xml`, build all services (assuming a multi-module setup or individual builds):
    ```bash
    mvn clean install -DskipTests
    ```
    *Note: Ensure `easy-bom` is installed first if it's a separate project.*

3.  **Start with Docker Compose**
    Navigate to the docker-compose directory and start the services.
    ```bash
    cd docker-compose/default
    docker-compose up -d
    ```

    This will start:
    *   Databases (MySQL)
    *   Message Broker (Kafka)
    *   Observability Stack (Grafana, Loki, Tempo, Prometheus)
    *   Config Server & Eureka Server
    *   All Microservices (Accounts, Loans, Cards, Gateway, Message)

4.  **Access the Application**
    *   **Gateway**: `http://localhost:8072`
    *   **Eureka Dashboard**: `http://localhost:8070`
    *   **Grafana**: `http://localhost:3000` (Login: `admin`/`admin` - if default)
    *   **Keycloak**: `http://localhost:7080`

### API Documentation (Swagger)

Once the services are up, you can access the aggregated API documentation via the Gateway or individual services:
*   **Accounts**: `http://localhost:8081/swagger-ui.html`
*   **Loans**: `http://localhost:8083/swagger-ui.html`
*   **Cards**: `http://localhost:8082/swagger-ui.html`

## 📊 Observability

This project uses the modern **LGTM** stack from Grafana Labs.
*   Access **Grafana** at `http://localhost:3000` to visualize metrics, logs, and traces.
*   **Tempo** is configured for distributed tracing to visualize the request flow across microservices.
*   **Loki** aggregates logs from all containers.

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## 📄 License

This project is open-sourced software licensed under the [MIT license](https://opensource.org/licenses/MIT).
