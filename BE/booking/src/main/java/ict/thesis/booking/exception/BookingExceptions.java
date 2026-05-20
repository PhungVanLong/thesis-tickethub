package ict.thesis.booking.exception;

public final class BookingExceptions {

    private BookingExceptions() {
    }

    public static class BookingException extends RuntimeException {
        public BookingException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends BookingException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends BookingException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class ConflictException extends BookingException {
        public ConflictException(String message) {
            super(message);
        }
    }
}


