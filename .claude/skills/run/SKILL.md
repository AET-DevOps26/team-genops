# /run — Start services locally

Start the full stack or a specific service for local development.

## Usage

```
/run [service]
```

No argument → full stack via Docker Compose. Optional `service` argument: `web-client`, `auth`, `genai`, or any service name from `docker-compose.yml`.

## Steps

### Full stack (default)

1. Check that `.env` exists. If it does not, copy `.env.example` and tell the user to fill in the required values before proceeding.
2. Run:
   ```sh
   docker compose up --build
   ```
3. After containers are up, confirm `auth` is healthy:
   ```sh
   curl -s http://localhost:8080/actuator/health
   ```

### Individual service (outside Docker, for hot-reload development)

**web-client:**
```sh
cd web-client && npm install && npm run dev
```

**auth (Spring Boot):**
```sh
cd services/auth && ./mvnw spring-boot:run
```

**genai (Python / FastAPI):**
```sh
cd services/genai && pip install -r requirements.txt && uvicorn src.main:app --reload --port 8000
```

## Service URLs

| Service    | URL                                    |
|------------|----------------------------------------|
| web-client | http://localhost:5173                  |
| auth       | http://localhost:8080                  |
| auth Swagger | http://localhost:8080/swagger-ui.html |
| postgres   | localhost:5432                         |
| pgadmin    | http://localhost:5050                  |

## Notes

- Always verify `.env` is populated before starting the stack.
- If a service fails to start, check logs with `docker compose logs <service>`.
- `pgadmin` is a dev-only tool — never include it in Kubernetes manifests.
