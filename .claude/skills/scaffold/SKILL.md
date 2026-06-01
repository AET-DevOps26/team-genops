# /scaffold — Scaffold a new service or module

Generate the boilerplate skeleton for a new Spring Boot microservice or a new GenAI FastAPI module.

## Usage

```
/scaffold <type> <name>
```

- `type`: `service` (Spring Boot) or `genai-module` (Python FastAPI endpoint group)
- `name`: kebab-case name, e.g. `application`, `email`, `document`

## Scaffold: Spring Boot microservice

Creates the following under `services/<name>/`:

```
services/<name>/
├── Dockerfile
├── Dockerfile.dev
├── .dockerignore
├── pom.xml                          ← inherits project Java version; includes spring-boot-starter-web,
│                                       spring-boot-starter-actuator, springdoc-openapi, spring-security
├── mvnw  (copy from services/auth)
├── src/
│   ├── main/java/com/genops/<name>/
│   │   ├── <Name>Application.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java  ← JWT verification using auth public key
│   │   ├── controller/
│   │   │   └── HealthController.java
│   │   └── service/
│   └── test/java/com/genops/<name>/
│       └── <Name>ApplicationTests.java
└── src/main/resources/
    └── application.yml              ← server.port, spring.datasource (if needed), management.endpoints
```

After generating files:
1. Add the service to `docker-compose.yml` with an appropriate port and healthcheck.
2. Add the service's DB user and schema to `services/db/schema.sql` (if the service needs a DB).
3. Add new env vars to `.env.example`.
4. Add the service to `api/openapi.yaml` with at least a `/health` path.

## Scaffold: GenAI FastAPI module

Creates the following under `services/genai/src/`:

```
services/genai/src/routers/<name>.py   ← APIRouter with stub endpoint(s)
services/genai/tests/test_<name>.py    ← pytest stubs for the new router
```

After generating:
1. Register the router in `services/genai/src/main.py`: `app.include_router(<name>.router, prefix="/api/v1")`
2. Add the new endpoint(s) to `api/openapi.yaml`.

## Rules

- Use the same Java package prefix as the rest of the project: `com.genops.<name>`.
- All Spring Boot services must expose `/actuator/health` for the Docker Compose healthcheck.
- All Spring Boot services must re-verify the JWT — never trust a service-to-service call without verifying the token.
- Every scaffold is a starting point — generated stubs must be replaced with real logic before merging to main.
