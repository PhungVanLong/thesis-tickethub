## 3.3 Use Cases
### 3.3.1 Use Cases Diagram
The following diagram describes the functions that a user can perform with the system.
*(Figure 3.1: Use Cases Diagram)*

## 3.4 Use Case and Scenario Description

### 3.4.1 Authentication & Account Management

**Use case 1: Login**
*   **Actor:** Customer, Organizer, Staff, and Admin
*   **Description:** This use case describes the process by which a user logs into the system using their email along with a valid password.
*   **Precondition:** The user must already have a registered account in the system.
*   **Post-condition:** The user is successfully authenticated and redirected to their corresponding dashboard.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Actor opens the login interface. | 2. The system displays fields for Email and Password. | |
| 3. The Actor enters their credentials. | 4. The system verifies that all required fields are filled. | - Email<br>- Password |
| 5. The Actor submits the form. | 6. The system validates credentials against the database. | |
| | 7. The system authenticates the Actor and redirects them to their corresponding dashboard. | - JWT Token |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Invalid Email or Password | The system will reload and display an error message: “Invalid email or password.” |
| Account Locked | The system displays an error message stating the account is locked due to too many failed attempts and suggests the user to reset their password via email. |

**Special Requirements:**
*   The system must encrypt all passwords stored using a secure hashing algorithm (Bcrypt).
*   The system prevents brute-force login attempts by limiting failed login retries.

---

### 3.4.2 Organization Onboarding Flow

**Use case 2: Register Organization**
*   **Actor:** Customer
*   **Description:** This Use Case allows a Customer to submit their organization profile (e.g., Tax Code, Name) to request an account upgrade to the Organizer role.
*   **Precondition:** The Customer must be logged in and does not already hold the Organizer role.
*   **Post-condition:** An organization registration request is created with "Pending" status, awaiting Admin approval.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Customer goes to the "Become an Organizer" page. | 2. The system displays the registration form. | |
| 3. The Customer enters Organization Name, Tax Code, Email, and Phone. | 4. The system verifies that all required fields are filled. | - Org Name*<br>- Tax Code*<br>- Email*<br>- Phone* |
| 5. The Customer submits the form. | 6. The system checks if the Tax Code is unique in the database. | |
| | 7. The system saves the request as "Pending" and notifies the Admin. | |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Tax Code Already Exists | The system displays a warning message: "This Tax Code is already registered" and highlights the field. |
| Missing Required Information | The system displays a validation error prompting the user to fill in the missing fields. |

---

**Use case 3: Verify Organization Profile**
*   **Actor:** Admin
*   **Description:** This Use Case allows the Admin to review the submitted tax codes and organization details to approve or reject the upgrade to Organizer.
*   **Precondition:** The Admin must be logged in. A registration request exists in "Pending" status.
*   **Post-condition:** The Customer's role is upgraded to Organizer or the request is rejected.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Admin goes to the Organization Verification page. | 2. The system displays a list of pending registration requests. | - List of pending profiles |
| 3. The Admin clicks on a request to view details. | 4. The system displays the Organization Name, Tax Code, and Contact details. | - Org details |
| 5. The Admin clicks "Approve". | 6. The system displays a confirmation dialog. | - Request ID |
| 7. The Admin confirms. | 8. The system updates the Customer's role to Organizer, assigns Organizer permissions, and sends an approval email. | |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Admin clicks "Reject" | The system prompts the Admin to enter a rejection reason. The request is marked as "Rejected" and an email is sent to the Customer. |

---

### 3.4.3 Event Management Flow

**Use case 4: Create Event**
*   **Actor:** Organizer
*   **Description:** This Use Case allows the Organizer to create a new event, including details, seat map configuration, and pricing.
*   **Precondition:** The Organizer must be logged into the system.
*   **Post-condition:** A new event is created with "Pending" status waiting for Admin approval.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Organizer goes to the Event Management page and clicks "Create Event". | 2. The system displays a form to enter event information. | |
| 3. The Organizer enters event details and uploads a banner. | 4. The system verifies required fields. | - Event Name*<br>- Start/End Time*<br>- Location*<br>- Description<br>- Banner Image |
| 5. The Organizer configures the seat map and ticket tiers. | 6. The system validates the pricing and seat capacity. | - Tier Name*<br>- Price*<br>- Quantity* |
| 7. The Organizer submits the event. | 8. The system saves the event as "Pending" and notifies the Admin. | |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Missing Required Information | The system highlights the invalid fields and displays a warning message. |
| Invalid Start/End Time | The system displays an error if the End Time is before the Start Time. |

---

**Use case 5: Approve Event**
*   **Actor:** Admin
*   **Description:** This Use Case allows the Admin to review and approve or reject an event submitted by an Organizer.
*   **Precondition:** The Admin must be logged in. An event must exist in "Pending" status.
*   **Post-condition:** The event status changes to "Approved" (ready to publish) or "Rejected".

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Admin goes to the Event Approval page. | 2. The system displays a list of pending events. | - List of pending events |
| 3. The Admin clicks on an event to view details. | 4. The system displays the full event information, seat map, and pricing. | - Event details |
| 5. The Admin clicks "Approve". | 6. The system displays a confirmation dialog. | - Event ID |
| 7. The Admin confirms. | 8. The system updates the event status to "Approved" and notifies the Organizer. | |

---

**Use case 6: Publish Event**
*   **Actor:** Organizer
*   **Description:** After the Admin approves the event, the Organizer publishes it to make tickets available for sale.
*   **Precondition:** The Event must be in "Approved" status.
*   **Post-condition:** The event status changes to "Published" and is visible to Customers.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Organizer goes to the Approved Events list. | 2. The system displays events ready for publication. | - Event ID |
| 3. The Organizer clicks "Publish" on a specific event. | 4. The system displays a confirmation dialog warning that seat maps cannot be easily changed after publication. | |
| 5. The Organizer confirms. | 6. The system updates the status to "Published", triggers a Kafka event to prepare necessary event data, and pushes SSE notifications to followers. | |

---

### 3.4.4 Booking & Check-in Flow

**Use case 7: Book Tickets**
*   **Actor:** Customer
*   **Description:** This Use Case allows the Customer to select tickets for an event and proceed to checkout.
*   **Precondition:** The Customer must be logged in and the Event must be active/selling.
*   **Post-condition:** An order is created and the user is redirected to the payment gateway.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Customer clicks "Book Now" on an event page. | 2. The system displays the seat map and ticket tiers. | - Event ID |
| 3. The Customer selects desired seats or ticket quantities and clicks "Checkout". | 4. The system validates availability using a pessimistic lock (or distributed lock). | - Selected Seats<br>- Ticket Tiers |
| 5. The system temporarily reserves the tickets. | 6. The system displays the Order Summary and total amount. | - Subtotal<br>- Total Amount |
| 7. The Customer selects a payment method and clicks "Pay". | 8. The system creates an Order and redirects the Customer to the integrated Payment Gateway. | - Payment Gateway |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Seat Already Taken | The system displays a warning: "The selected seat is no longer available" and asks the Customer to re-select. |
| Payment Timeout | If the payment is not completed within 15 minutes, the system automatically cancels the order and releases the reserved tickets. |

---

**Use case 8: Check Attendance (Check-in)**
*   **Actor:** Staff
*   **Description:** The Staff records attendee entrance by scanning the E-ticket QR code. The system validates the ticket and updates the check-in status.
*   **Precondition:** The Staff is logged in and assigned to the event.
*   **Post-condition:** The ticket status is marked as "USED" and check-in history is saved.

**Basic Flow:**

| Actor Action | System Action | Data |
| :--- | :--- | :--- |
| 1. The Staff opens the Check-in page for the event. | 2. The system activates the device camera to scan QR codes. | - Event ID |
| 3. The Staff scans the Customer's QR code. | 4. The system extracts the ticket code and queries the database with a pessimistic lock. | - Ticket Code |
| 5. The system verifies the ticket is valid and "UNUSED". | 6. The system updates the ticket status to "USED", records the check-in log, and returns a Success message. | - Device ID<br>- Staff ID |
| 7. The Staff allows the Customer to enter. | 8. The system publishes an event to Kafka to update real-time analytics. | |

**Alternative Flow:**

| Condition / Actor Action | System Response |
| :--- | :--- |
| Ticket is "USED" | The system displays a RED error message: "Ticket has already been used!" to prevent double entry. |
| Invalid Ticket Code | The system displays a RED error message: "Ticket not found or invalid." |
