# Booking Entities

This module contains JPA entities generated from the ERD image under `src/main/java/ict/thesis/booking/enties`.

## Simple booking flow

The application now exposes a simple end-to-end booking API:

- `POST /api/bookings` — create a booking, process payment, and issue tickets in one transaction
- `GET /api/bookings/{orderId}` — fetch booking details

### Example request

```json
{
  "customerId": 1,
  "idempotencyKey": "abc-123",
  "voucherCode": "SUMMER10",
  "gatewayName": "mock-gateway",
  "gatewayTxId": "TX-001",
  "currency": "VND",
  "items": [
	{
	  "seatId": 1,
	  "ticketTierId": 1,
	  "promotionId": 1
	}
  ]
}
```

## Notes

- Enums are mapped to PostgreSQL named enums via `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- If your DB enum values differ, update the enum constants in `ict.thesis.booking.enties.enums`.

