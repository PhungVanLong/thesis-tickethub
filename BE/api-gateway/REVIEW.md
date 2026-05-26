# API Gateway Review

Date: 2026-05-26
Scope: Entire api-gateway project

## Overview
This review covers routing, JWT validation, security posture, configuration, and build/dependencies for the api-gateway service.

## Findings
### High
- None identified in current codebase.

### Medium
- Gateway uses `spring-boot-starter-security` without explicit WebFlux security configuration. This can cause unexpected 403 responses. A SecurityWebFilterChain that permits requests (or defines intended auth rules) is required to avoid default denies.
- Two JWT filters exist (`AuthenticationFilter` and `JwtGlobalAuthenticationFilter`) with overlapping responsibilities. Keeping both may cause confusion or inconsistent behavior. Prefer one source of truth (global filter) and ensure route configs align.

### Low
- Route URI mismatch risk: service IDs must match `spring.application.name` in downstream services to avoid 5xx from discovery.
- JWT secret is hardcoded in `application.properties`. Consider externalized config for non-dev environments.
- Dependencies `spring-cloud-starter-gateway` and `spring-cloud-gateway-server` show deprecation warnings. Consider migrating to `spring-cloud-starter-gateway-server-webflux`.

## Recommended Actions
1. Add WebFlux security configuration to explicitly permit/secure endpoints as intended.
2. Consolidate JWT validation to a single filter (prefer the global filter) and align route configuration accordingly.
3. Verify service IDs in routes match Eureka registrations for all downstream services.
4. Move JWT secret to environment variables or a secret manager.
5. Update gateway dependency to the non-deprecated WebFlux starter.

## Notes
- JWT validation should be centralized in the gateway; downstream services can be configured to trust gateway headers or remain permissive.
- Ensure `/api/auth/**` remains publicly accessible while securing all other paths.

