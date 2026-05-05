## Identity API (FE)

Base URL: `http://localhost:8080`

### 1. Register

`POST /api/auth/register`

Request body:
```json
{
	"email": "user@example.com",
	"password": "12345678",
	"fullName": "Nguyen Van A",
	"phone": "0900000000"
}
```

Response 200:
```json
{
	"accessToken": "<jwt>",
	"tokenType": "Bearer",
	"expiresInSeconds": 604800
}
```

Errors:
- 409: `{ "code": "CONFLICT", "message": "Email already exists", ... }`
- 400: `{ "code": "VALIDATION_ERROR", "message": "Validation failed", ... }`

### 2. Login

`POST /api/auth/login`

Request body:
```json
{
	"email": "user@example.com",
	"password": "12345678"
}
```

Response 200:
```json
{
	"accessToken": "<jwt>",
	"tokenType": "Bearer",
	"expiresInSeconds": 604800
}
```

Errors:
- 401: `{ "code": "UNAUTHORIZED", "message": "Invalid credentials", ... }`

### 3. Authorization Header

For protected APIs, add header:

`Authorization: Bearer <jwt>`

### 4. Error format

```json
{
	"code": "VALIDATION_ERROR",
	"message": "Validation failed",
	"details": {
		"fields": {
			"email": "must be a well-formed email address"
		}
	},
	"timestamp": "2026-05-05T10:00:00Z",
	"path": "/api/auth/register"
}
```
