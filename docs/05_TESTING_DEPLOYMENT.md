# TicketHub Testing Strategy & Deployment Procedures

**Version:** 1.0  
**Date:** 2026-04-21

---

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Unit Testing](#unit-testing)
3. [Integration Testing](#integration-testing)
4. [Load Testing](#load-testing)
5. [Security Testing](#security-testing)
6. [Acceptance Criteria](#acceptance-criteria)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Production Deployment](#production-deployment)
9. [Post-Deployment Validation](#post-deployment-validation)

---

## Testing Strategy Overview

### Testing Pyramid

```
         /\
        /  \  API/End-to-End Tests (10%)
       /____\  - User journeys
      /      \
     / E2E    \  
    /__________|
   /            \
  /  Integration \  Integration Tests (30%)
 /   Tests (30%)  \ - Service interactions
/________________|
                  \
    Unit Tests    \  Unit Tests (60%)
    (60%)         \  - Business logic
__________________\ - Isolated components
```

### Test Coverage Goals

| Category | Current | Target | Timeline |
|----------|---------|--------|----------|
| Unit Tests | 0% | 80% | Month 2 |
| Integration Tests | 0% | 60% | Month 3 |
| API Contract Tests | 0% | 100% | Month 1 |
| Load Tests | Manual | Automated CI | Month 4 |
| Security Tests | Manual | Automated CI | Month 5 |

---

## Unit Testing

### Testing Framework

**Dependencies:**
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.9.x</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.2.x</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.2.x</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.24.x</version>
    <scope>test</scope>
</dependency>
```

### Unit Test Examples

#### Example 1: BookingService

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private SeatService seatService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private BookingService bookingService;
    
    @Test
    @DisplayName("Should create booking with valid seats")
    void testCreateBooking_Success() {
        // Given
        long eventId = 1;
        List<Long> seatIds = List.of(101L, 102L, 103L);
        CreateBookingRequest request = new CreateBookingRequest(eventId, seatIds);
        
        // When
        ArgumentCaptor<Booking> argumentCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(any())).thenAnswer(
            invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(5001L);
                return booking;
            }
        );
        
        BookingResponse response = bookingService.createBooking(request, 123L);
        
        // Then
        verify(bookingRepository).save(argumentCaptor.capture());
        Booking savedBooking = argumentCaptor.getValue();
        
        assertThat(savedBooking).isNotNull();
        assertThat(savedBooking.getEventId()).isEqualTo(eventId);
        assertThat(savedBooking.getCustomerId()).isEqualTo(123L);
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getExpiresAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Should throw exception when seat limit exceeded")
    void testCreateBooking_ExceedsLimit() {
        // Given
        long eventId = 1;
        List<Long> seatIds = new ArrayList<>();
        for (int i = 0; i < 11; i++) {  // Max 10 seats per booking
            seatIds.add((long) i);
        }
        CreateBookingRequest request = new CreateBookingRequest(eventId, seatIds);
        
        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request, 123L))
            .isInstanceOf(BookingException.class)
            .hasMessage("Maximum 10 seats allowed per booking");
    }
    
    @Test
    @DisplayName("Should handle idempotency key correctly")
    void testCreateBooking_Idempotent() {
        // Given
        String idempotencyKey = "550e8400-e29b-41d4-a716-446655440000";
        CreateBookingRequest request = new CreateBookingRequest(1L, List.of(101L, 102L));
        request.setIdempotencyKey(idempotencyKey);
        
        // First call
        when(bookingRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.empty());
        
        BookingResponse response1 = bookingService.createBooking(request, 123L);
        
        // Second call (should return same result)
        when(bookingRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.of(response1.toEntity()));
        
        BookingResponse response2 = bookingService.createBooking(request, 123L);
        
        // Then
        assertThat(response1.getBookingId()).isEqualTo(response2.getBookingId());
    }
}
```

#### Example 2: SeatLockingService

```java
@ExtendWith(MockitoExtension.class)
class SeatLockingServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private SeatRepository seatRepository;
    
    @InjectMocks
    private SeatLockingService seatLockingService;
    
    @Test
    @DisplayName("Should successfully lock seat")
    void testLockSeat_Success() {
        // Given
        long seatId = 101L;
        long bookingId = 5001L;
        int lockDurationSeconds = 300;
        
        // When
        when(redisTemplate.opsForValue().setIfAbsent(
            any(String.class),
            any(),
            any(Duration.class)
        )).thenReturn(true);
        
        LockResult result = seatLockingService.lockSeat(seatId, bookingId, lockDurationSeconds);
        
        // Then
        assertThat(result.isLocked()).isTrue();
        assertThat(result.getSeatId()).isEqualTo(seatId);
        assertThat(result.getLockedUntil()).isNotNull();
    }
    
    @Test
    @DisplayName("Should fail to lock already locked seat")
    void testLockSeat_AlreadyLocked() {
        // Given
        long seatId = 101L;
        long bookingId = 5001L;
        
        // When
        when(redisTemplate.opsForValue().setIfAbsent(
            any(String.class),
            any(),
            any(Duration.class)
        )).thenReturn(false);
        
        LockResult result = seatLockingService.lockSeat(seatId, bookingId, 300);
        
        // Then
        assertThat(result.isLocked()).isFalse();
        assertThat(result.getError()).contains("already locked");
    }
    
    @Test
    @DisplayName("Should verify lock ownership before unlocking")
    void testUnlockSeat_VerifyOwnership() {
        // Given
        String lockKey = "seat:101";
        String ownerLockValue = "owner-uuid-1234";
        
        // When
        when(redisTemplate.opsForValue().get(lockKey))
            .thenReturn(ownerLockValue);
        
        boolean unlocked = seatLockingService.unlockSeat(lockKey, ownerLockValue);
        
        // Then
        assertThat(unlocked).isTrue();
        verify(redisTemplate).delete(lockKey);
    }
}
```

### Running Unit Tests

```bash
# Run all unit tests
mvn clean test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# CI Pipeline (GitHub Actions)
- name: Run Unit Tests
  run: mvn clean test -DskipITs
```

---

## Integration Testing

### Testing Framework

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.17.x</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.17.x</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.17.x</version>
    <scope>test</scope>
</dependency>
```

### Integration Test Examples

#### Example: Booking Service with Containers

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BookingServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"));
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Test
    @DisplayName("Should create booking end-to-end")
    void testCreateBooking_EndToEnd() {
        // Given
        CreateBookingRequest request = new CreateBookingRequest(
            1L,
            List.of(101L, 102L, 103L),
            "550e8400-e29b-41d4-a716-446655440000"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + generateValidToken());
        HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/bookings",
            HttpMethod.POST,
            entity,
            BookingResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("PENDING");
        
        // Verify saved to database
        Booking savedBooking = bookingRepository.findById(response.getBody().getBookingId()).orElse(null);
        assertThat(savedBooking).isNotNull();
        assertThat(savedBooking.getCustomerId()).isEqualTo(123L);
    }
    
    @Test
    @DisplayName("Should handle payment event and complete booking")
    void testBookingCompletion_OnPaymentEvent() throws InterruptedException {
        // Given
        Booking booking = new Booking(1L, 123L, BookingStatus.PENDING, 120.00);
        bookingRepository.save(booking);
        
        // When: Publish payment.captured event
        PaymentCapturedEvent event = new PaymentCapturedEvent(
            booking.getId(),
            9001L,
            booking.getTotalPrice(),
            Instant.now()
        );
        kafkaTemplate.send("ticketing.payment.events", event);
        
        // Then: Wait for async processing
        Thread.sleep(2000);
        
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElse(null);
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }
    
    private String generateValidToken() {
        // Generate JWT token with valid credentials
        return Jwts.builder()
            .setSubject("123")
            .claim("role", "CUSTOMER")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(SignatureAlgorithm.HS256, "your-secret-key")
            .compact();
    }
}
```

### Running Integration Tests

```bash
# Run integration tests only
mvn verify -DskipUnitTests

# Run with Docker
docker-compose up -d
mvn verify

# Clean up
docker-compose down
```

---

## Load Testing

### JMeter Test Plan

**Configuration:**
```
Target Load: 5,000 concurrent users
Ramp-up: 10 minutes (500 users/min)
Duration: 30 minutes
Think Time: 2-3 seconds between requests
```

**Test Scenarios:**

1. **Booking Scenario (40% of traffic)**
```
1. View Events (GET /events)
2. View Event Details (GET /events/{id})
3. View Seat Map (GET /events/{id}/seats)
4. Lock Seat (POST /events/{id}/seats/{seatId}/lock)
5. Create Booking (POST /bookings)
6. Initiate Payment (POST /payments/initiate)
```

2. **Payment Callback Scenario (30% of traffic)**
```
1. Payment webhook (POST /payments/webhook/vnpay)
2. Get booking status (GET /bookings/{id})
3. Download ticket (GET /tickets/{id}/download)
```

3. **Check-in Scenario (20% of traffic)**
```
1. Validate ticket (POST /checkins)
2. Get check-in history (GET /checkins/history)
```

4. **Analytics Scenario (10% of traffic)**
```
1. View event analytics (GET /analytics/events/{id}/stats)
2. System dashboard (GET /analytics/system-health)
```

**JMeter Script (XML):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testname="TicketHub Load Test" enabled="true">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments"/>
    </TestPlan>
    
    <ThreadGroup guiclass="ThreadGroupGui" testname="Load Test - 5000 Users">
      <stringProp name="ThreadGroup.num_threads">5000</stringProp>
      <stringProp name="ThreadGroup.ramp_time">600</stringProp> <!-- 10 minutes -->
      <stringProp name="ThreadGroup.duration">1800</stringProp> <!-- 30 minutes -->
    </ThreadGroup>
    
    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="GET /events">
      <HTTPsampler.domain>api.tickethub.local</HTTPsampler.domain>
      <HTTPsampler.port>443</HTTPsampler.port>
      <HTTPsampler.protocol>https</HTTPsampler.protocol>
      <HTTPsampler.path>/api/v1/events?page=1&page_size=20</HTTPsampler.path>
      <HTTPsampler.method>GET</HTTPsampler.method>
    </HTTPSamplerProxy>
  </hashTree>
</jmeterTestPlan>
```

**Expected Results:**
```
- P50 latency: < 100ms
- P95 latency: < 300ms
- P99 latency: < 800ms
- Error rate: < 0.1%
- Throughput: 200+ requests/second
- Database connections: < 80% of pool
- Memory usage: < 85%
- CPU usage: < 75%
```

### Running Load Tests

```bash
# Start infrastructure
docker-compose up -d postgres redis kafka

# Wait for services to start
sleep 30

# Run load test
jmeter -n -t load-test-tickethub.jmx -l results.jtl -j jmeter.log

# Analyze results
jmeter -g results.jtl -o ./jmeter-report/

# Generate HTML report
firefox ./jmeter-report/index.html
```

---

## Security Testing

### OWASP Top 10 Checks

#### 1. SQL Injection Test

```bash
# Test with malicious input
curl -X POST https://api.tickethub.io/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin'\''; DROP TABLE users; --",
    "password": "any"
  }'

# Expected: Input validation error, not database error
```

#### 2. XSS Prevention Test

```bash
# Test with XSS payload
curl -X POST https://api.tickethub.io/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "<script>alert(\"XSS\")</script>",
    "description": "Test"
  }'

# Expected: Payload escaped or rejected
```

#### 3. CSRF Protection Test

```bash
# Test without CSRF token
curl -X POST https://api.tickethub.io/bookings \
  -H "Content-Type: application/json" \
  -d '{...}'

# Expected: 403 Forbidden or CSRF token required
```

#### 4. Authentication Bypass Test

```bash
# Test without token
curl -X GET https://api.tickethub.io/users/bookings

# Expected: 401 Unauthorized

# Test with invalid token
curl -X GET https://api.tickethub.io/users/bookings \
  -H "Authorization: Bearer invalid_token"

# Expected: 401 Unauthorized
```

#### 5. Authorization Bypass Test

```bash
# Test accessing another user's data
curl -X GET https://api.tickethub.io/users/456/bookings \
  -H "Authorization: Bearer <user_123_token>"

# Expected: 403 Forbidden
```

### OWASP Dependency Check

```bash
# Scan dependencies for CVEs
mvn org.owasp:dependency-check-maven:check

# Report location: target/dependency-check-report.html
```

### API Security Scanner

```bash
# Using ZAPROXY
zaproxy -cmd \
  -quickurl https://api.tickethub.io \
  -quickout /tmp/zap-report.html
```

---

## Acceptance Criteria

### Booking Feature

```gherkin
Feature: Booking Management

  Scenario: Customer successfully books seats
    Given a customer is logged in
    And event "Concert 2026" has available seats
    When customer selects 3 seats
    And customer confirms booking
    Then booking should be created with PENDING status
    And seats should be locked for 5 minutes
    And customer should receive confirmation email

  Scenario: Booking expires if not paid
    Given a booking is created with PENDING status
    When 5 minutes pass without payment
    Then booking should be marked EXPIRED
    And seats should be unlocked
    And customer notification should be sent
```

### Payment Feature

```gherkin
Feature: Payment Processing

  Scenario: Customer completes payment successfully
    Given a booking with PENDING status exists
    When customer initiates payment via VNPAY
    And payment gateway approves transaction
    Then booking status should change to COMPLETED
    And e-ticket should be generated
    And payment event should be published to Kafka
    And order confirmation email should be sent with ticket

  Scenario: Handle payment timeout
    Given customer initiates payment
    When payment gateway does not respond within 30 seconds
    Then API should return timeout error
    And booking should remain PENDING
    And customer should be able to retry
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
name: TicketHub CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
      
      redis:
        image: redis:7
      
      kafka:
        image: confluentinc/cp-kafka:7.4.0
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: 17
          distribution: temurin
      
      - name: Run Unit Tests
        run: mvn clean test -DskipITs
      
      - name: Run Integration Tests
        run: mvn verify
      
      - name: SonarQube Analysis
        run: mvn sonar:sonar -Dsonar.projectKey=tickethub
      
      - name: Build Docker Image
        run: |
          docker build -t $REGISTRY/$IMAGE_NAME:${{ github.sha }} .
          docker push $REGISTRY/$IMAGE_NAME:${{ github.sha }}
      
      - name: Run Security Scan
        run: |
          mvn org.owasp:dependency-check-maven:check
          trivy image $REGISTRY/$IMAGE_NAME:${{ github.sha }}
      
      - name: Upload Artifacts
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: target/site/

  deploy-staging:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Staging
        run: |
          kubectl set image deployment/tickethub-services \
            -n staging \
            service=$REGISTRY/$IMAGE_NAME:${{ github.sha }}
          kubectl rollout status deployment/tickethub-services -n staging

  deploy-production:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Deploy to Production
        run: |
          kubectl set image deployment/tickethub-services \
            -n production \
            service=$REGISTRY/$IMAGE_NAME:${{ github.sha }}
          kubectl rollout status deployment/tickethub-services -n production
      
      - name: Post-Deployment Smoke Tests
        run: |
          ./scripts/smoke-tests.sh https://api.tickethub.io
      
      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Production deployment complete'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] All tests passing (unit, integration, e2e)
- [ ] Code review approved
- [ ] Security scan passed
- [ ] Performance benchmarks met
- [ ] Database migrations tested
- [ ] Rollback plan documented
- [ ] Monitoring configured
- [ ] Incident response plan ready
- [ ] Stakeholder approval obtained

### Blue-Green Deployment

```bash
#!/bin/bash
# deploy.sh

set -e

NAMESPACE="production"
SERVICE_NAME="tickethub-services"
NEW_IMAGE="ghcr.io/tickethub:v1.5.0"

# 1. Scale up green deployment
kubectl set image deployment/${SERVICE_NAME}-green \
  -n $NAMESPACE \
  service=$NEW_IMAGE

# 2. Wait for rollout
kubectl rollout status deployment/${SERVICE_NAME}-green \
  -n $NAMESPACE \
  --timeout=5m

# 3. Run smoke tests against green
./scripts/smoke-tests.sh https://green-api.tickethub.io

# 4. Switch traffic (service selector)
kubectl patch service $SERVICE_NAME \
  -n $NAMESPACE \
  -p '{"spec":{"selector":{"version":"green"}}}'

# 5. Monitor for errors
sleep 60
ERROR_RATE=$(curl -s https://monitoring.tickethub.io/api/v1/errors | jq '.error_rate')

if (( $(echo "$ERROR_RATE > 1" | bc -l) )); then
  # Rollback
  kubectl patch service $SERVICE_NAME \
    -n $NAMESPACE \
    -p '{"spec":{"selector":{"version":"blue"}}}'
  exit 1
fi

echo "Deployment successful"
```

---

## Post-Deployment Validation

### Smoke Tests

```bash
#!/bin/bash
# smoke-tests.sh

API_URL=$1

echo "Running smoke tests against $API_URL"

# Test 1: API Gateway health
echo "Test 1: API Gateway health"
curl -f $API_URL/health || exit 1

# Test 2: Event listing
echo "Test 2: Event listing"
EVENTS=$(curl -s $API_URL/api/v1/events | jq '.data | length')
if [ "$EVENTS" -gt 0 ]; then
  echo "✓ Events API working"
else
  echo "✗ Events API failed"
  exit 1
fi

# Test 3: Authentication
echo "Test 3: Authentication"
TOKEN=$(curl -s -X POST $API_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test"}' \
  | jq '.data.access_token')

if [ -n "$TOKEN" ]; then
  echo "✓ Auth API working"
else
  echo "✗ Auth API failed"
  exit 1
fi

# Test 4: Database connectivity
echo "Test 4: Database connectivity"
DB_TIME=$(curl -s $API_URL/actuator/metrics/db.connection.active | jq '.measurements[0].value')
if [ -n "$DB_TIME" ]; then
  echo "✓ Database connected"
else
  echo "✗ Database connection failed"
  exit 1
fi

echo "All smoke tests passed ✓"
```

---

**END OF TESTING STRATEGY & DEPLOYMENT PROCEDURES**

