# Chapter 4. Methodology

## 4.1 Tools and Techniques

This section shows the main tools used to build the TicketHub system. Each tool was chosen because it helps make the system fast, safe, and able to handle many users at the same time.

### 4.1.1 Postman
Postman is a tool for building and testing APIs. It is used extensively to check the REST APIs of the backend microservices. It helps to send test requests, check the responses, and make sure different services can talk to each other correctly before connecting them to the frontend.

### 4.1.2 Angular
Angular is a framework for building web applications using HTML and TypeScript. Angular is used to build the frontend of TicketHub because it is easy to organize and has many helpful tools. It helps create fast and smooth user interfaces for all users (Customer, Organizer, Admin, Staff). It is very good for complex pages like seat selection maps and live dashboards.

### 4.1.3 PostgreSQL
PostgreSQL is a strong, free, and open-source database system. It is very safe and fast. In TicketHub, PostgreSQL is used as the main database. It has great features like Database Locks (Pessimistic Locking). This is very important to stop errors when many people try to buy the same ticket at the same time. It makes sure no ticket is sold twice.

### 4.1.4 Spring Boot
Spring Boot is a framework that makes it easy to build Java applications. It is used to build the backend microservices for TicketHub. Spring Boot was chosen because it is fast to set up and has many useful libraries (like Spring Data JPA and Spring Security). It helps build independent services (Identity, Booking, Management) quickly. Each service can also be run and updated easily.

### 4.1.5 Apache Kafka
Apache Kafka is a tool for sending and receiving messages between services very quickly. In this project, Kafka is used as the main message broker. It helps the microservices talk to each other in the background (asynchronous communication). For example, it sends ticket booking updates to the dashboard in real time. This keeps the system running fast for the user without waiting for heavy background tasks to finish.

### 4.1.6 JSON Web Token (JWT)
JSON Web Token (JWT) is a standard way to send secure information as a JSON object. TicketHub uses JWT to manage user logins. When a user logs in, the system gives them a JWT. The user sends this token with their next requests. This helps the microservices check who the user is very fast, without asking the database every time.

### 4.1.7 Role-Based Access Control (RBAC)
Role-Based Access Control (RBAC) is a way to limit what users can do based on their roles. In TicketHub, RBAC is used with Spring Security and JWT. The system has clear rules for different users (Customer, Organizer, Staff, Admin). For example, only an Admin can approve new organizers, and Staff can only check tickets for their assigned events. This keeps the data safe and private.

### 4.1.8 Spring Cloud
Spring Cloud gives developers tools to build microservices easily. In TicketHub, Spring Cloud is used to manage the system. It works as an API Gateway to receive requests from the Angular frontend and route them to the correct backend service.

### 4.1.9 Server-Sent Events (SSE)
Server-Sent Events (SSE) is a tool that helps the server send real-time data to the user. It is used in TicketHub to show live updates on the dashboard. For example, when staff scan a ticket, the backend sends the new data straight to the Angular frontend. The user can see the charts and seat maps update immediately without reloading the page.

### 4.1.10 Docker
Docker is a tool that helps developers pack and run applications in containers. Docker is used in TicketHub to run heavy tools like Apache Kafka, Zookeeper, and PostgreSQL. Running these tools in Docker containers makes the setup process much easier. It keeps the development environment clean and makes sure the project runs exactly the same way everywhere.


