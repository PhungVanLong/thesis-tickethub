# Sử dụng Eureka trong API Gateway

Tài liệu ngắn này giải thích cách API Gateway trong project sử dụng Eureka để discover và chuyển tiếp (route) tới các service.

1) Cấu hình chính

- Gateway bật tính năng discovery locator và cấu hình Eureka trong [src/main/resources/application.properties](src/main/resources/application.properties#L6-L7).

  Ví dụ quan trọng trong `application.properties`:

```
spring.cloud.gateway.server.webflux.discovery.locator.enabled=true
spring.cloud.gateway.server.webflux.discovery.locator.lower-case-service-id=true

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.fetch-registry=true
eureka.client.register-with-eureka=true
```

2) Cách gateway route đến service

- Thay vì gọi URL cố định, gateway sử dụng định danh service và `lb://<serviceId>` để chuyển tiếp tới instance được Eureka đăng ký. Các route mẫu nằm trong [src/main/resources/application.properties](src/main/resources/application.properties#L10-L22).

  Ví dụ route:

```
spring.cloud.gateway.server.webflux.routes[1].uri=lb://identity
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/auth/login, /api/auth/register
```

3) Cơ chế hoạt động (ngắn gọn)

- Khi request tới gateway với path phù hợp predicate, Spring Cloud Gateway sử dụng DiscoveryClient (Eureka) để lấy list instance của `identity`.
- Router sẽ chọn instance (load-balancer) và forward request tới instance đó. Không cần hard-code hostname/port trong gateway.

4) Kiểm thử nhanh

- Chạy Eureka server (nếu chưa có) và đảm bảo services đăng ký thành công.
- Gửi request tới gateway:

```bash
curl -i http://localhost:8080/api/auth/login
```

Nếu service `identity` đã đăng ký và route cấu hình đúng, gateway sẽ chuyển tiếp request tới instance `identity`.

5) Tham chiếu mã nguồn

- Cấu hình route & discovery: [src/main/resources/application.properties](src/main/resources/application.properties)
- Lọc JWT toàn cục: [src/main/java/ict/thesis/api_gateway/filter/JwtGlobalAuthenticationFilter.java](src/main/java/ict/thesis/api_gateway/filter/JwtGlobalAuthenticationFilter.java)
- Entrypoint ứng dụng: [src/main/java/ict/thesis/api_gateway/ApiGatewayApplication.java](src/main/java/ict/thesis/api_gateway/ApiGatewayApplication.java)

6) Nếu muốn gọi một service cụ thể

- Thông thường không cần. Nếu bạn muốn gọi trực tiếp một instance cụ thể (ví dụ để debug), bạn có thể gọi trực tiếp `http://{host}:{port}/...` tới service, nhưng trong production để trông chờ vào load-balancer/Eureka là tốt hơn.

---
Tôi đã tạo file này trong `docs/EUREKA_USAGE.md`. Muốn tôi mở rộng phần cấu hình load-balancer (Ribbon/Reactive LB), hoặc thêm ví dụ cấu hình cho một service mới không?
