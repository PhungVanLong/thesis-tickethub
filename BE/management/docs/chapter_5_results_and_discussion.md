# Chapter 5. Results and Discussion

## 5.1 System Demonstration
This section presents the final implementation of the TicketHub system, showcasing the user interfaces and core functionalities developed for different user roles.

### 5.1.1 Customer Interface
- **Home & Event Discovery:** Customers are provided with an intuitive homepage to browse ongoing and upcoming events. Search and filtering functionalities allow users to find events efficiently by category, date, or location.
- **Booking & Payment:** The booking process features an interactive seat map where users can select their preferred seats or ticket tiers. The integration with secure payment gateways ensures a seamless checkout experience. The system utilizes Server-Sent Events (SSE) to provide real-time status updates without requiring page reloads.
- **E-Ticket Management:** Upon successful payment, electronic tickets (E-Tickets) containing unique and secure QR codes are instantly generated and stored in the customer's profile for easy access during event entry.

### 5.1.2 Organizer Dashboard
- **Event Management:** Organizers have access to a comprehensive dashboard to create new events, configure pricing tiers, and design detailed seat maps. Events are submitted for administrator approval before being published.
- **Staff Management:** Organizers can efficiently manage operational staff accounts, assigning them to specific events for check-in duties.
- **Analytics & Reporting:** The dashboard provides real-time statistics on ticket sales performance, revenue generation, and attendance, enabling organizers to make data-driven decisions.

### 5.1.3 Staff & Admin Interfaces
- **Staff Check-in System:** Event staff utilize a dedicated scanner interface to quickly validate E-Ticket QR codes at the venue. This prevents ticket duplication and updates attendance status instantly. A manual check-in fallback is also provided in case of hardware failures.
- **Admin Verification:** Administrators have a centralized dashboard to monitor the platform. They review, verify, and approve organization registrations (as detailed in the Outbox and Saga patterns) and event publications to maintain platform quality and compliance.

## 5.2 System Performance Evaluation
To validate the system's capability to handle high-concurrency ticket sales (as outlined in the initial objectives), performance and architectural evaluations were conducted.

- **High-Concurrency Booking:** The implementation of an event-driven architecture, combined with database-level pessimistic locks and Kafka message queues, effectively handles high volumes of concurrent booking requests. The system successfully prevents over-selling and data loss under peak loads.
- **Data Consistency and Asynchronous Processing:** By implementing the Transactional Outbox pattern and Idempotent Consumers, the system ensures strict data integrity across decoupled microservices (Management, Event, Booking, Identity). This approach prevents distributed transaction deadlocks and significantly reduces response times for end-users.

## 5.3 Discussion
The developed TicketHub system successfully addresses the objectives set out in Chapter 2, delivering a fully functional, highly scalable web-based event ticketing platform.

- **Fulfillment of Requirements:** The platform provides a robust solution encompassing the entire event lifecycle, from initial organization onboarding to the final attendee check-in process. The microservices architecture allows each functional domain to operate and scale independently.
- **Advantages:** The utilization of modern architectural patterns (Saga, Outbox) ensures high availability, fault tolerance, and reliable state recovery during service failures. The real-time feedback mechanisms enhance the overall user experience during asynchronous operations.
- **Limitations:** Currently, the system relies on specific integrated payment gateways, which might limit flexibility for some international transactions. Furthermore, while the dynamic seat map rendering is optimized, extremely large and complex stadium layouts may require further frontend performance tuning for lower-end devices.

