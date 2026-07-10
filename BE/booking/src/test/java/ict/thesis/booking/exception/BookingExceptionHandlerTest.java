// package ict.thesis.booking.exception;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;

// import ict.thesis.booking.dto.BookingDtos.ApiErrorResponse;
// import org.junit.jupiter.api.Test;
// import org.springframework.http.ResponseEntity;
// import org.springframework.mock.web.MockHttpServletRequest;

// class BookingExceptionHandlerTest {

//     private final BookingExceptionHandler handler = new BookingExceptionHandler();

//     @Test
//     void conflictExceptionShouldReturn409() {
//         MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/bookings");

//         ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
//                 new BookingExceptions.ConflictException("Ghế không khả dụng"),
//                 request
//         );

//         assertEquals(409, response.getStatusCode().value());
//         assertNotNull(response.getBody());
//         assertEquals("Ghế không khả dụng", response.getBody().message());
//         assertEquals("/api/bookings", response.getBody().path());
//     }
// }

