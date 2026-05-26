# Code review: DataInitializer va cac khu vuc lien quan

## Pham vi

- [src/main/java/ict/thesis/identity/config/DataInitializer.java](src/main/java/ict/thesis/identity/config/DataInitializer.java)
- [src/main/java/ict/thesis/identity/controller/AuthController.java](src/main/java/ict/thesis/identity/controller/AuthController.java)
- [src/main/java/ict/thesis/identity/service/AuthService.java](src/main/java/ict/thesis/identity/service/AuthService.java)
- [src/main/java/ict/thesis/identity/security/SecurityConfig.java](src/main/java/ict/thesis/identity/security/SecurityConfig.java)
- [src/main/java/ict/thesis/identity/security/JwtService.java](src/main/java/ict/thesis/identity/security/JwtService.java)
- [src/main/java/ict/thesis/identity/security/JwtAuthenticationFilter.java](src/main/java/ict/thesis/identity/security/JwtAuthenticationFilter.java)
- [src/main/java/ict/thesis/identity/security/RestAccessDeniedHandler.java](src/main/java/ict/thesis/identity/security/RestAccessDeniedHandler.java)
- [src/main/java/ict/thesis/identity/security/RestAuthenticationEntryPoint.java](src/main/java/ict/thesis/identity/security/RestAuthenticationEntryPoint.java)
- [src/main/java/ict/thesis/identity/security/UserDetailsServiceImpl.java](src/main/java/ict/thesis/identity/security/UserDetailsServiceImpl.java)
- [src/main/java/ict/thesis/identity/entity/User.java](src/main/java/ict/thesis/identity/entity/User.java)
- [src/main/java/ict/thesis/identity/repository/UserRepository.java](src/main/java/ict/thesis/identity/repository/UserRepository.java)
- [src/main/java/ict/thesis/identity/dto/RegisterRequest.java](src/main/java/ict/thesis/identity/dto/RegisterRequest.java)
- [src/main/java/ict/thesis/identity/dto/LoginRequest.java](src/main/java/ict/thesis/identity/dto/LoginRequest.java)
- [src/main/java/ict/thesis/identity/dto/AuthResponse.java](src/main/java/ict/thesis/identity/dto/AuthResponse.java)
- [src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java](src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java)
- [src/main/java/ict/thesis/identity/exception/ApiException.java](src/main/java/ict/thesis/identity/exception/ApiException.java)
- [src/main/java/ict/thesis/identity/exception/ApiErrorResponse.java](src/main/java/ict/thesis/identity/exception/ApiErrorResponse.java)
- [src/main/java/ict/thesis/identity/exception/ConflictException.java](src/main/java/ict/thesis/identity/exception/ConflictException.java)
- [src/main/java/ict/thesis/identity/exception/UnauthorizedException.java](src/main/java/ict/thesis/identity/exception/UnauthorizedException.java)

## Tong quan nhanh

- Luong auth: Controller -> Service -> Repository, token duoc tao qua JwtService va duoc kiem tra o JwtAuthenticationFilter.
- Exception duoc map thanh response JSON thong nhat qua GlobalExceptionHandler.
- DataInitializer tao admin mac dinh khi app chay lan dau.

## Review annotation (muc dich va ly do su dung)

### Config va khoi tao du lieu

- [@Configuration tren DataInitializer](src/main/java/ict/thesis/identity/config/DataInitializer.java#L16)
  - Muc dich: danh dau class la cau hinh Spring, cho phep khai bao bean.
  - Ly do: can tao `CommandLineRunner` de chay logic khoi tao sau khi app start.
- [@Bean cho initAdminUser](src/main/java/ict/thesis/identity/config/DataInitializer.java#L23)
  - Muc dich: dang ky `CommandLineRunner` vao context.
  - Ly do: tu dong chay khoi tao admin khi ung dung khoi dong.

### Controller va validation

- [@RestController + @RequestMapping](src/main/java/ict/thesis/identity/controller/AuthController.java#L17-L19)
  - Muc dich: khai bao REST endpoint va base path.
  - Ly do: gom nhom cac endpoint auth duoi `/api/auth`.
- [@Validated tren controller](src/main/java/ict/thesis/identity/controller/AuthController.java#L20)
  - Muc dich: bat validation cho cac input.
  - Ly do: ket hop `@Valid` de trigger validation cho request body.
- [@Valid + @RequestBody](src/main/java/ict/thesis/identity/controller/AuthController.java#L25-L31)
  - Muc dich: map JSON vao DTO va validate field.
  - Ly do: dam bao request hop le truoc khi xu ly logic.

### DTO validation

- [@Email, @NotBlank, @Size trong RegisterRequest](src/main/java/ict/thesis/identity/dto/RegisterRequest.java#L7-L12)
  - Muc dich: kiem soat format va do dai du lieu dau vao.
  - Ly do: bao ve dau vao cho register, giam loi du lieu.
- [@Email, @NotBlank trong LoginRequest](src/main/java/ict/thesis/identity/dto/LoginRequest.java#L6-L9)
  - Muc dich: kiem soat input login.
  - Ly do: ngan request khong hop le den layer auth.

### Entity va JPA

- [@Entity + @Table cho User](src/main/java/ict/thesis/identity/entity/User.java#L17-L24)
  - Muc dich: map class vao table `users`.
  - Ly do: su dung JPA de tu dong tao schema va query.
- [@Id + @GeneratedValue](src/main/java/ict/thesis/identity/entity/User.java#L29-L31)
  - Muc dich: khoa chinh tu tang.
  - Ly do: phu hop IDENTITY cho MySQL/Postgres thong dung.
- [@Column, @Enumerated](src/main/java/ict/thesis/identity/entity/User.java#L33-L54)
  - Muc dich: map cot DB va enum role.
  - Ly do: dam bao ranh buoc du lieu (nullable, unique).
- [@CreationTimestamp, @UpdateTimestamp](src/main/java/ict/thesis/identity/entity/User.java#L60-L66)
  - Muc dich: tu dong gan thoi gian tao/cap nhat.
  - Ly do: tranh code thu cong o service cho audit field.

### Security

- [@Configuration trong SecurityConfig](src/main/java/ict/thesis/identity/security/SecurityConfig.java#L15)
  - Muc dich: cau hinh security filter chain va bean auth.
  - Ly do: gom tat ca security bean vao 1 noi.
- [@Bean cho SecurityFilterChain, PasswordEncoder, AuthenticationManager](src/main/java/ict/thesis/identity/security/SecurityConfig.java#L34-L68)
  - Muc dich: khai bao cac thanh phan bat buoc cua Spring Security.
  - Ly do: co the inject vao service va filter.
- [@Service cho JwtService va UserDetailsServiceImpl](src/main/java/ict/thesis/identity/security/JwtService.java#L16), [UserDetailsServiceImpl](src/main/java/ict/thesis/identity/security/UserDetailsServiceImpl.java#L15-L17)
  - Muc dich: dang ky service cho Spring.
  - Ly do: can inject vao auth layer va filter.
- [@Component cho JwtAuthenticationFilter, entrypoint, access denied](src/main/java/ict/thesis/identity/security/JwtAuthenticationFilter.java#L22), [RestAuthenticationEntryPoint](src/main/java/ict/thesis/identity/security/RestAuthenticationEntryPoint.java#L19), [RestAccessDeniedHandler](src/main/java/ict/thesis/identity/security/RestAccessDeniedHandler.java#L19)
  - Muc dich: bien cac class thanh bean de co the add vao filter chain.
  - Ly do: gom xu ly loi auth va authz ve JSON.

### Exception handler

- [@RestControllerAdvice](src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java#L18)
  - Muc dich: bat exception toan cuc cho REST.
  - Ly do: dong nhat response error va giam logic try/catch o controller.
- [@ExceptionHandler](src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java#L22-L77)
  - Muc dich: map tung loai exception sang response.
  - Ly do: tra ve JSON co code + message + timestamp.

## Review logic chi tiet (va ly do)

### DataInitializer

- [Kiem tra admin ton tai](src/main/java/ict/thesis/identity/config/DataInitializer.java#L29-L31)
  - Ly do: tranh tao trung admin neu da khoi tao.
  - Diem manh: dung repository `existsByEmail` de check nhanh.
- [Tao user bang builder](src/main/java/ict/thesis/identity/config/DataInitializer.java#L33-L41)
  - Ly do: builder ro rang va de mo rong.
  - Diem can chu y: set `createdAt` thu cong trong khi entity co `@CreationTimestamp`.
- [Log sau khi tao admin](src/main/java/ict/thesis/identity/config/DataInitializer.java#L43-L44)
  - Ly do: giup trace log khoi tao he thong.

### AuthService

- [Register: check trung email](src/main/java/ict/thesis/identity/service/AuthService.java#L34-L36)
  - Ly do: ngan conflict truoc khi save.
- [Password encoder + role mac dinh](src/main/java/ict/thesis/identity/service/AuthService.java#L38-L46)
  - Ly do: luu hash thay vi plain text, set role theo default.
- [Login: authenticationManager](src/main/java/ict/thesis/identity/service/AuthService.java#L57-L59)
  - Ly do: tai su dung co che auth cua Spring Security.
- [Tra token sau login](src/main/java/ict/thesis/identity/service/AuthService.java#L65-L68)
  - Ly do: ho tro stateless auth tren client.

### Security flow

- [SecurityFilterChain cau hinh stateless](src/main/java/ict/thesis/identity/security/SecurityConfig.java#L37-L49)
  - Ly do: JWT khong can session, can filter truoc username/password filter.
- [JwtAuthenticationFilter logic parse token](src/main/java/ict/thesis/identity/security/JwtAuthenticationFilter.java#L40-L66)
  - Ly do: lay username tu token, build `Authentication` neu hop le.
- [JwtService tao token va verify](src/main/java/ict/thesis/identity/security/JwtService.java#L33-L61)
  - Ly do: gom logic JWT vao 1 service, de test va reuse.
- [RestAuthenticationEntryPoint + RestAccessDeniedHandler](src/main/java/ict/thesis/identity/security/RestAuthenticationEntryPoint.java#L24-L40), [RestAccessDeniedHandler](src/main/java/ict/thesis/identity/security/RestAccessDeniedHandler.java#L24-L40)
  - Ly do: tra ve JSON thay vi HTML mac dinh cua Spring Security.

### Exception handling

- [GlobalExceptionHandler cho ApiException](src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java#L22-L38)
  - Ly do: map loi domain thanh response co code ro rang.
- [Validation error gom field errors](src/main/java/ict/thesis/identity/exception/GlobalExceptionHandler.java#L40-L59)
  - Ly do: de front-end biet field nao sai.

## Nhung diem can xem lai (risk va de xuat)

1) Mat khau admin mac dinh hard-code
- [DEFAULT_ADMIN_PASSWORD](src/main/java/ict/thesis/identity/config/DataInitializer.java#L20-L21) la gia tri co dinh.
- Risk: de lo mat khau neu deploy that.
- De xuat: lay tu env/secret manager va buoc doi mat khau sau login dau.

2) createdAt duoc set tay trong DataInitializer
- [set createdAt](src/main/java/ict/thesis/identity/config/DataInitializer.java#L40) trong khi entity da co `@CreationTimestamp`.
- Risk: khong nhat quan giua data seed va record tao tu service.
- De xuat: bo set `createdAt` thu cong de DB/JPA tu gan.

3) Role trong JWT chi la string
- [claim role](src/main/java/ict/thesis/identity/security/JwtService.java#L41)
- Risk: neu role change, token cu van con gia tri.
- De xuat: can xem xet deny danh sach role cu, hoac invalidate token khi role update.

4) Catch chung AuthenticationException
- [AuthService login catch](src/main/java/ict/thesis/identity/service/AuthService.java#L69-L73)
- Risk: mat thong tin loai loi (disabled, locked, ...).
- De xuat: mapping rieng cho tung exception neu can thong bao ro hon.

5) ObjectMapper tao moi trong handler
- [ObjectMapper new](src/main/java/ict/thesis/identity/security/RestAccessDeniedHandler.java#L21), [RestAuthenticationEntryPoint](src/main/java/ict/thesis/identity/security/RestAuthenticationEntryPoint.java#L21)
- Risk: khong dung ObjectMapper bean chung (config Jackson).
- De xuat: inject `ObjectMapper` bean qua constructor.

6) Validation cho phone
- [RegisterRequest phone](src/main/java/ict/thesis/identity/dto/RegisterRequest.java#L11)
- Risk: khong kiem soat format/length.
- De xuat: them `@Pattern`/`@Size` neu can.

## Diem tot can giu

- Luong auth va jwt ro rang, tach layer tot.
- Exception response dong nhat va co `code` + `timestamp`.
- Su dung BCrypt cho password.
- DTO validation co ban day du cho email/password.

## Goi y cai tien nhe (khong bat buoc)

- Them response cho `register` tra thong tin user (non-sensitive) neu can.
- Them refresh token neu can session dai.
- Them audit log cho login that bai.
