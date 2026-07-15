import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 options configuration
export const options = {
    vus: __ENV.VUS ? parseInt(__ENV.VUS) : 100, // Default to 100 Virtual Users
    duration: __ENV.DURATION || '30s',         // Default to 30 seconds duration
    thresholds: {
        // Performance requirements under load
        http_req_duration: ['p(95)<1500'], // Allow up to 1.5s for 95% of requests
        http_req_failed: ['rate<0.05'],   // Error rate must be less than 5%
    },
};

// Target Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // Gateway URL
const EVENT_ID = __ENV.EVENT_ID || '1';                     // Default Event ID

export default function () {
    // 1. Test Event Details Endpoint (Public GET through Gateway)
    const eventUrl = `${BASE_URL}/api/events/${EVENT_ID}`;
    const eventRes = http.get(eventUrl, {
        headers: { 'Accept': 'application/json' }
    });
    
    check(eventRes, {
        'GET Event Details status is 200': (r) => r.status === 200,
        'GET Event Details response time < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(1); // Think time between requests (1 second)

    // 2. Test Seat Maps Endpoint
    const seatMapUrl = `${BASE_URL}/api/events/${EVENT_ID}/seat-maps`;
    const seatMapRes = http.get(seatMapUrl, {
        headers: { 'Accept': 'application/json' }
    });

    check(seatMapRes, {
        'GET Seat Maps status is 200': (r) => r.status === 200,
        'GET Seat Maps response time < 300ms': (r) => r.timings.duration < 300,
    });

    sleep(1); // Think time before next iteration
}
