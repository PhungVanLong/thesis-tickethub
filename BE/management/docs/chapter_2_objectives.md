# Chapter 2. Objectives

In this segment, we will define the standard requirements of the project, provide a brief overview of the function of the system and the reasons for its development.

## 2.1 Desired Features
The main goal of this project is to develop a high-performance web-based system designed to manage event ticketing, real-time seat booking, secure payments, and event operations with basic services:

*   **FEATURE 1: Authentication and Access Control**
    *   **SUB-FEATURE 1.1: Registration and Login:** Allow users to create accounts and log in securely using their email or username.
    *   **SUB-FEATURE 1.2: Role-based Access Management:** Distinguish functionalities for Customers, Organizers, Staff, and Administrators within a unified platform.
    *   **SUB-FEATURE 1.3: Organization Registration:** Allow authenticated customers to submit an organization profile and required documents to request an upgrade to the Event Organizer role.

*   **FEATURE 2: Customer Interface**
    *   **SUB-FEATURE 2.1: Search and View Events:** Allow users to search, filter, and explore upcoming events, view detailed descriptions, and check real-time seat availability.
    *   **SUB-FEATURE 2.2: Book Tickets and Payment:** Allow customers to select ticket tiers or specific seats on a map, initiate asynchronous booking, and securely complete transactions via integrated third-party payment gateways.
    *   **SUB-FEATURE 2.3: Manage E-Tickets:** Allow customers to receive, store, and manage their purchased electronic tickets equipped with secure QR codes for event entry.

*   **FEATURE 3: Organizer Dashboard**
    *   **SUB-FEATURE 3.1: Manage Events:** Allow the organizer to create new events, configure detailed seat maps, set pricing tiers, edit event details, and submit them for administrator approval.
    *   **SUB-FEATURE 3.2: Manage Staff Accounts:** Allow the organizer to manage operational staff accounts, including creating profiles and assigning them to specific events for check-in duties.
    *   **SUB-FEATURE 3.3: View Analytics Reports:** Allow the organizer to monitor ticket sales performance, revenue generation, and attendee statistics through real-time dashboards.

*   **FEATURE 4: Staff Dashboard**
    *   **SUB-FEATURE 4.1: QR Code Check-in:** Allow event staff to quickly scan E-Ticket QR codes at the gate to verify ticket validity, prevent duplicate entries, and update attendance status instantly.
    *   **SUB-FEATURE 4.2: Manual Check-in:** Allow staff to manually verify and check-in attendees using unique ticket codes in case of QR scanning hardware failures.
    *   **SUB-FEATURE 4.3: View Scan History:** Allow staff to view the history of successfully checked-in tickets during an ongoing event.

*   **FEATURE 5: Admin Dashboard**
    *   **SUB-FEATURE 5.1: Approve Organizations:** Allow the administrator to review, verify, and approve or reject organization registration requests to maintain platform quality.
    *   **SUB-FEATURE 5.2: Approve Events:** Allow the administrator to review submitted events for policy compliance before authorizing their publication to the public platform.
    *   **SUB-FEATURE 5.3: View System Reports:** Allow the administrator to oversee system-wide metrics, manage user accounts, and resolve platform-level issues.

## 2.2 Expected Outcome
The project aims to deliver a fully functional, highly scalable web-based event ticketing system with the following key achievements:

*   **High-Concurrency Ticket Sales Handling** – Ensures the system can process thousands of simultaneous booking requests without data loss or over-selling through an event-driven architecture and distributed locking mechanisms.
*   **Secure & Reliable Architecture** – Ensures strict data integrity and consistency across decoupled microservices using the Transactional Outbox pattern, Idempotent Consumers, and database-level pessimistic locks.
*   **Real-time User Feedback** – Allows customers to instantly track their asynchronous booking status and receive immediate payment confirmations via Server-Sent Events (SSE) without page reloads.
*   **Streamlined Event Management** – Enables organizers to efficiently handle complex multi-tier seat maps, automate staff assignments, and track sales revenue seamlessly.
*   **Optimized User Experience** – Delivers a modern and responsive interface with intuitive navigation, dynamic seat selection, and reliable E-Ticket generation for all user roles.
