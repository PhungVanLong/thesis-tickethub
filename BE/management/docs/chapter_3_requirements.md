# Chapter 3. Requirement Analysis

In this chapter, we will examine a brief review of the project’s functions and the system’s scenarios and use cases. This section includes the entire functional and non-functional specification tailored for a high-concurrency event ticketing platform.

## 3.1 Overall System Requirements
In general, this application should satisfy the following requirements:
*   A login system with authentication and strict role-based access control.
*   The user can view, update, and manage event ticketing data according to their specific role permissions.
*   The system limits users with distinct features:
    *   **For Administrators:**
        *   Manage organizational structures by reviewing and approving/rejecting Organizer registration requests.
        *   Manage platform content by reviewing and approving new events to ensure compliance before publication.
        *   Monitor system-wide metrics, oversee transactions, and manage user accounts.
    *   **For Organizers:**
        *   Manage event configurations, including descriptions, multi-tier seat maps, and ticket pricing.
        *   Manage event publication and monitor real-time ticket sales, revenue, and attendee analytics via a comprehensive dashboard.
        *   Manage staff accounts by creating profiles and assigning them to specific events for check-in operations.
    *   **For Staff:**
        *   View the list of events and check-in tasks assigned to them.
        *   Perform attendance validation by scanning E-ticket QR codes or entering codes manually at the event gates.
        *   View real-time check-in history and attendee status during ongoing events to resolve entry disputes.
    *   **For Customers:**
        *   Search for upcoming events, view detailed information, and check real-time seat availability.
        *   Initiate asynchronous ticket booking and securely process payments via integrated third-party payment gateways.
        *   View personal order history, track payment status, and manage purchased E-tickets (QR codes) for event entry.

## 3.2 Users & Non-functional Requirements
*   **Users Requirement:** Have a device with internet access and a valid account to fully interact with the system. Guests have limited access.
*   **Usability:** The web interface is designed to be intuitive and easy to navigate, ensuring that users can comfortably browse events and book tickets.
*   **Security:** The system secures user accounts by encrypting passwords and managing sessions via JSON Web Tokens (JWT). Role-Based Access Control (RBAC) is strictly enforced at the API level to prevent unauthorized actions.
*   **Data Integrity:** The system utilizes database transactions and relational constraints to guarantee that critical operations, such as ticket booking and payment status updates, maintain consistent states without data corruption.
*   **Maintainability:** The backend is structured into modular microservices (e.g., Booking, Management, Identity), allowing individual components to be updated or debugged independently without affecting the entire system.
*   **Reliability:** The system will not work without an Internet connection.
