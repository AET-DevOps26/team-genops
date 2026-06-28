# Auth Service

Spring Boot service responsible for user registration, authentication, and token management.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login with email and password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout and invalidate refresh token |
| GET  | `/api/v1/auth/me` | Get current user info |

Full contract: [`api/openapi.yaml`](../../api/openapi.yaml) — Swagger UI available at `http://localhost:8080/swagger-ui.html` when running.

## Run

```bash
# With Docker (recommended)
docker compose up --build auth

# Without Docker
cd services/auth && ./mvnw spring-boot:run
```

Runs on `http://localhost:8080`.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

## Structure

```
src/main/java/com/jobready/auth/
├── AuthApplication.java
├── config/          # SecurityConfig
├── controller/      # HTTP layer — delegates to service
├── service/         # Business logic (AuthService / AuthServiceImpl)
├── modelEntity/     # JPA entities
├── repository/      # Spring Data repositories
├── exception/       # GlobalExceptionHandler, domain exceptions
└── generated/       # Auto-generated from api/openapi.yaml — do not edit
    ├── api/
    └── modelDto/
```

To regenerate after spec changes:
```bash
make -C api generate
```
