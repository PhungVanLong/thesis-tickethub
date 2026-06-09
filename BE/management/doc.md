# Management - Domain & Business Docs

## 1) Domain Overview
Core entities and relationships:
- RefUser: legacy referenced user with a role (still used by some admin flows).
- Events: event created by organizer; main aggregate.
- EventApprovals: admin decision for event approval.
- EventStaff: staff assignment to event with a role.
- SeatMap: seating map for an event.
- TicketTier: ticket tier linked to a seat map.
- Seat: seat instance linked to a seat map and ticket tier.
- AnalyticsEvent: event-level metrics snapshot.

## 2) Business Rules (Core)
- Create event:
  - Only organizer (or authorized role) can create.
  - Default status: DRAFT.
  - isPublished = false by default.
- Approval:
  - Admin creates EventApprovals with APPROVED or REJECTED.
  - Approved event can be published.
- Publishing:
  - Only APPROVED event can be published.
- Ticketing:
  - Seat status flow: AVAILABLE -> HELD -> BOOKED -> CHECKED_IN.
  - TicketTier.quantityAvailable must never go below 0.
- Analytics:
  - Metrics are stored as snapshots by time.

## 3) Create Event - Step by Step
This section describes how to implement the "create event" feature using current entities.

### Step 1: DTOs
Create request/response DTOs to decouple API from entities.
- Request: organizerId, title, description, venue, city, locationCoords, startTime, endTime, bannerUrl.
- Response: id, status, createdAt, updatedAt.

### Step 2: Repositories
Create repositories for the required aggregates:
- EventsRepository
- RefUserRepository (legacy/admin support)
(Optional: SeatMapRepository, TicketTierRepository if created in one flow.)

### Step 3: Service Layer
Add EventService with createEvent(CreateEventRequest request):
1) Validate organizer exists.
2) Validate startTime < endTime and title not empty.
3) Build Events entity with status DRAFT, isPublished = false.
4) Save Events.
5) If seat map or ticket tiers are included, save them linked to Events.
6) Return CreateEventResponse.

### Step 4: Controller
Expose POST /api/events:
- Input: CreateEventRequest
- Output: CreateEventResponse
- Return 201 Created on success.

### Step 5: Tests (Minimal)
- Integration test to create event:
  - Expect status = DRAFT, organizer set, timestamps set.
- If seat map/tier creation is included, assert related rows exist.

## 4) Validation Checklist
- organizerId exists
- title not blank
- startTime and endTime are valid and ordered
- seat map and tiers are consistent (if provided)

## 5) Notes
- Current entity IDs are Long; if switching to UUID, update DTOs and repositories accordingly.
- locationCoords is currently a String; if migrating to Postgres POINT, add a custom type mapping.

