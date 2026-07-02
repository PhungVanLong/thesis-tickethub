## 4.2 System Architecture

This section explains the complete structure of the TicketHub system in detail. The architecture is built using the Microservices pattern to handle high traffic, ensure data safety, and allow different parts to be updated independently.

### 4.2.1 Overall Architecture Diagram

The following diagram shows the high-level architecture of the TicketHub platform. It illustrates how the client layer connects to the backend services through the API Gateway, and how services communicate using Kafka.

```mermaid
graph TD
    %% Define Styles
    classDef client fill:#f9f,stroke:#333,stroke-width:2px;
    classDef gateway fill:#ff9,stroke:#333,stroke-width:2px;
    classDef service fill:#bbf,stroke:#333,stroke-width:2px;
    classDef db fill:#bfb,stroke:#333,stroke-width:2px;
    classDef kafka fill:#fbb,stroke:#333,stroke-width:2px;

    Client[Frontend<br/>(Angular)]:::client

    subgraph "Gateway Layer"
        Gateway[API Gateway<br/>(Spring Cloud Gateway)]:::gateway
    end

    subgraph "Microservices"
        Identity[Identity Service<br/>(Java Spring)]:::service
        Management[Management Service<br/>(Java Spring)]:::service
        Booking[Booking Service<br/>(Java Spring)]:::service
    end

    subgraph "Data & Event Messaging"
        DB_Id[(Identity DB<br/>(PostgreSQL))]:::db
        DB_Mgmt[(Management DB<br/>(PostgreSQL))]:::db
        DB_Book[(Booking DB<br/>(PostgreSQL))]:::db
        Kafka{Message Broker<br/>(Kafka)}:::kafka
        SMTP[SMTP Server]:::db
        PaymentGateway[Payment Gateway]:::db
    end

    %% Client Connections
    Client -->|"REST"| Gateway
    Management -.->|"SSE (Check-in)"| Client
    Booking -.->|"SSE (Booking)"| Client

    %% Gateway to Services
    Gateway -->|"Route"| Identity
    Gateway -->|"Route"| Management
    Gateway -->|"Route"| Booking

    %% Databases
    Identity --> DB_Id
    Management --> DB_Mgmt
    Booking --> DB_Book

    %% Kafka Events
    Management <-.->|"Events"| Kafka
    Booking <-.->|"Events"| Kafka

    %% SMTP
    Management -->|"Email"| SMTP
    Booking -->|"Email"| SMTP

    %% Payment Integration
    Booking --> PaymentGateway
    PaymentGateway --> Booking
```

### 4.2.2 Gateway Layer

The system uses an Angular-based web application as the user interface. All REST HTTP requests from the frontend must go through the **API Gateway** (built with Spring Cloud Gateway). The Gateway Layer acts as the single entry point for the system. 

It performs two primary functions:
1. **Routing:** It receives incoming HTTP requests, analyzes the URL path, and dynamically routes them to the correct backend microservice.
2. **Security Checkpoint:** Before forwarding any request, it extracts and validates the JSON Web Token (JWT) from the request header to ensure the user is authenticated and possesses the correct permissions (Role-Based Access Control).

### 4.2.3 Microservices Layer

The core backend logic is divided into specialized microservices, all developed using the **Java Spring** framework. This modular design makes the system easier to manage and update without affecting the entire platform. The main microservices are:

*   **Identity Service:** Manages user accounts, handles authentication (login/registration), and issues JWT tokens.
*   **Management Service:** Organizes core event data, seat map configurations, staff assignments, and organizer profile approvals. It also handles the ticket scanning (check-in) process.
*   **Booking Service:** The most critical service, responsible for ticket selection, temporary seat holding (database locks), order creation, and asynchronous ticket purchasing logic.

### 4.2.4 Data & Event Messaging Layer

To ensure true independence and handle distributed communication, the bottom layer consists of data storage, message brokering, and external integrations:

*   **Database-per-Service (PostgreSQL):** Each microservice owns a dedicated PostgreSQL database (Identity DB, Management DB, Booking DB). For example, the Booking Service cannot directly read the Management DB. This prevents data coupling and ensures that if one database requires maintenance, the other services remain unaffected.
*   **Message Broker (Kafka):** Because databases are isolated, inter-service communication is handled asynchronously using an Event-Driven Architecture. For example, when an order is successfully paid, the Booking Service publishes an event message to Kafka. The Management Service consumes this event to update its dashboard statistics.
*   **SMTP Server:** The Management and Booking services integrate with an external SMTP Server to dispatch asynchronous emails (e.g., sending congratulatory emails to approved organizers or PDF E-Tickets to customers).
*   **Payment Gateway:** A critical external integration for the Booking Service. It handles transaction processing and communicates back with the system via IPN Webhooks to finalize asynchronous ticket allocations.

### 4.2.5 Real-Time Communication (Server-Sent Events)

To provide a modern, reactive user experience, the system utilizes **Server-Sent Events (SSE)** to push real-time updates from the microservices directly to the Angular frontend:
*   **SSE (Check-in):** When a staff member scans a ticket, the Management Service updates its database and immediately pushes live check-in statistics to the Organizer's dashboard via SSE.
*   **SSE (Booking):** During the asynchronous ticket purchasing flow, the Booking Service processes the queue in the background and uses SSE to instantly notify the user (ticket buyer) whether their booking was successful or if the seats were already taken. This purchasing flow is available to all non-admin users, meaning Organizers and Staff can also act as buyers.
