# CalmSkin - Backend Microservices Platform

[![Java Version](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-blue.svg?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue.svg?style=flat-square&logo=docker)](https://www.docker.com/)

CalmSkin is a modern, highly scalable, event-driven e-commerce and skincare personalization platform. It is built using a microservices architecture to provide skincare routines, personalized product recommendations, automated subscriptions, order management, and secure payments.

🌐 **Looking for the frontend?** Visit the [CalmSkin Frontend Repository](https://github.com/Lakeserl/CalmSkin-fe).

---

## 🏗 System Architecture & Communication Flow

The CalmSkin backend utilizes a **decentralized microservices architecture** structured around key enterprise patterns:

1. **Routing & Security Entry Point:** All client requests pass through the **Spring Cloud Gateway** (API Gateway). The Gateway performs global CORS management, rate-limiting, and intercepts requests to validate JSON Web Tokens (JWT) before forwarding downstream. External access to internal endpoints (prefixed with `/internal/**`) is strictly blocked.
2. **Service Discovery:** Microservice instances register themselves dynamically with the **Eureka Server** (Discovery Service). The API Gateway relies on Eureka for client-side load balancing (`lb://`) to route requests to healthy service nodes.
3. **Data Isolation (Database-per-Service):** Each microservice maintains its own dedicated schema within a shared **PostgreSQL** instance to enforce strict service boundaries. Database schemas and migrations are version-controlled and applied independently at startup by **Flyway**.
4. **Event-Driven Integration:** Critical transactions and cross-service actions communicate asynchronously via **Apache Kafka**. For instance, when an order is placed, the `order-service` publishes an event to Kafka. The `inventory-service` consumes it to reserve stock, while the `notification-service` consumes it to trigger order confirmation emails.
5. **Caching & Rate Limiting:** **Redis** is shared across services to manage distributed sessions, execute API rate limiting, and cache frequently read data (such as product categories or user sessions).
6. **Observability Stack:** Services expose detailed diagnostic data via **Spring Boot Actuator**. Trace information is propagated across network boundaries using **Micrometer Tracing** and collected by **Zipkin** for distributed transaction tracking. Operational metrics are scraped by **Prometheus** and visualized in **Grafana**.

---

## 🛠 Tech Stack

*   **Language & Core Framework:** Java 17, Spring Boot 3.4.5, Spring Cloud 2024.0.1
*   **Database & Migration:** PostgreSQL, Redis, Flyway
*   **Message Broker:** Apache Kafka (Confluent Platform)
*   **Observability & Tracing:** Actuator, Micrometer Tracing, Zipkin, Prometheus, Grafana
*   **APIs & Security:** Spring Security, JSON Web Tokens (JWT), SpringDoc OpenAPI (Swagger UI)
*   **Containerization:** Docker, Docker Compose

---

## 📁 Repository Structure & Services Detailed Breakdown

### Infrastructure Services

#### `discovery-service` (Eureka Server)
*   **Port:** `8761`
*   **Functionality:** Serves as the central service registry. Each microservice registers its IP address and port upon starting up. It monitors service health via heartbeats and allows other services to discover them dynamically, eliminating the need to hardcode service locations.

#### `api-gateway` (Spring Cloud Gateway)
*   **Port:** `8080`
*   **Functionality:** The single entry point for all external web and mobile clients.
    *   **Routing:** Dynamically routes client queries to downstream microservices based on paths (e.g., `/api/v1/products/**` goes to the product service).
    *   **Security:** Houses the `JwtAuthenticationFilter` which parses incoming authorization headers, checks signature validity, and maps roles. Rejects any incoming external traffic attempting to hit internal endpoints (`/internal/**`).
    *   **Resilience & Performance:** Integrates with Redis to enforce API rate limits and prevent brute-force or DDoS attacks.

---

### Core Business Microservices

#### `user-service`
*   **Port:** `8088`
*   **Functionality:** Responsible for user authentication, registration, profiles, and access control.
    *   Generates and validates JWTs, supports OAuth2 login configurations, and manages role permissions (Admin, User).
    *   Manages avatar uploads using AWS S3 with pre-signed URLs.
    *   Utilizes Redis to store OTPs, cache user information, and track active sessions.
    *   Publishes user registration and verification events to Kafka.

#### `product-service`
*   **Port:** `8086`
*   **Functionality:** Manages the skincare catalog, categories, ingredients, and customization logic.
    *   Houses the **Skincare Routine Builder** where users compile personalized skincare routines.
    *   Implements the recommendation engine matching user skin types to product ingredient properties.
    *   Saves brands, categories, and raw ingredients, offering comprehensive search capabilities.

#### `inventory-service`
*   **Port:** `8084`
*   **Functionality:** Handles stock inventory management and warehouse tracking.
    *   Tracks stock levels per product unit.
    *   Consumes order events from Kafka to reserve, deduct, or restore inventory when checkout cycles succeed or fail.
    *   Provides restock alerts and stock administration endpoints.

#### `order-service`
*   **Port:** `8082`
*   **Functionality:** Orchestrates the shopping cart checkout and order lifecycles.
    *   Manages order creations, transitions (Pending, Processing, Completed, Cancelled), and order history.
    *   Validates stock availability by querying the `inventory-service` via internal REST templates.
    *   Publishes `order-placed`, `order-cancelled`, and `order-completed` events to Kafka.

#### `payment-service`
*   **Port:** `8085`
*   **Functionality:** Integrates with payment processors (e.g., Stripe, VNPay).
    *   Processes payments and generates payment logs/receipts.
    *   Listens for order events, registers payment records, and updates payment state.
    *   Publishes `payment-success` or `payment-failed` events to Kafka to progress the order state machine.

#### `promotion-service`
*   **Port:** `8087`
*   **Functionality:** Manages discounts, discount codes, coupons, and vouchers.
    *   Determines promotion eligibility at checkout.
    *   Allows administrators to define conditions (e.g., minimum cart value, specific product categories) for vouchers.

#### `review-service`
*   **Port:** `8091`
*   **Functionality:** Collects and processes customer feedback, ratings, and product comments.
    *   Allows users to post product reviews and upload review media.
    *   Aggregates star-ratings to calculate the average rating score of products in real-time.
    *   Provides admin moderation tools for reviewing and approving user comments.

#### `shipping-service`
*   **Port:** `8090`
*   **Functionality:** Handles shipping carrier integrations, shipping fees, and package tracking.
    *   Calculates shipping costs based on geographical data.
    *   Updates order delivery states using webhook integrations from third-party shipping carriers.

#### `notification-service`
*   **Port:** `8089`
*   **Functionality:** Dispatches multi-channel notification alerts.
    *   An event-driven consumer that reads order, payment, and authentication events from Kafka.
    *   Dispatches HTML transactional emails (via AWS SES or SMTP servers).
    *   Supports Web Push notifications using VAPID keys.

#### `subscription-service`
*   **Port:** `8092`
*   **Functionality:** Handles beauty box subscriptions and recurring product orders.
    *   Processes periodic product delivery orders (e.g., monthly skincare kits).
    *   Maintains subscription schedules, active/paused subscription states, and automatically triggers recurring payments and order creation via Kafka integration.

---

## 🚀 Getting Started

### 📋 Prerequisites

Ensure you have the following installed on your machine:
*   [Docker](https://docs.docker.com/get-docker/) & [Docker Compose](https://docs.docker.com/compose/install/)
*   [Java Development Kit (JDK) 17+](https://adoptium.net/)
*   [Maven 3.8+](https://maven.apache.org/)

### 🔧 Configuration

1. Copy the `.env` template from the root directory:
   ```bash
   cp .env.example .env
   ```
2. Open the `.env` file and customize the variables, such as:
   *   `POSTGRES_USER` & `POSTGRES_PASSWORD`
   *   `JWT_SECRET`
   *   Mail server credentials (`MAIL_USERNAME`, `MAIL_PASSWORD`, etc.)

### 🐳 Running with Docker Compose

To start the infrastructure services (Postgres, Redis, Kafka, Zipkin) along with all microservices:

```bash
docker compose up -d
```

Check the health status of all containers:
```bash
docker compose ps
```

---

## 📊 Observability & Documentation

Once the services are running, you can access the following management dashboards:

*   **Eureka Discovery Dashboard:** [http://localhost:8761](http://localhost:8761)
*   **API Documentation (Swagger UI):** [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui) (aggregates API specs for all microservices)
*   **Distributed Tracing (Zipkin):** [http://localhost:9411](http://localhost:9411)
*   **Metrics & Dashboards (Grafana):** [http://localhost:3000](http://localhost:3000)

---

## 🤝 Contribution & License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.