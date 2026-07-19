# Gateway Service

> JobReady's API edge — a Spring Cloud Gateway that authenticates every `/api` request,
> translates the browser's HttpOnly access cookie into the mesh-standard `Authorization: Bearer`
> header, and routes traffic to the right backend service.

**Stack:** Spring Boot · Spring Cloud Gateway (WebMVC) · Spring Security OAuth2 Resource Server

---

## Why it exists

The browser edge and the service mesh speak two different auth dialects:

- **Browser → gateway:** the access JWT travels only as the `jr_access` cookie
  (`HttpOnly; Secure; SameSite=Strict`, set by the auth service) — never readable by JS.
- **Gateway → services:** the mesh contract is `Authorization: Bearer <jwt>` — services never
  see cookies.

The gateway bridges the two and enforces authentication **at the edge**, before any request
reaches a backend. It is the outer layer of defense in depth: every downstream service still
independently re-verifies the same JWT against auth's JWKS, so a request that somehow bypassed
the gateway would not be implicitly trusted.

## Request lifecycle

```
Browser ──(jr_access cookie)──▶ Gateway
   1. CookieToBearerFilter        cookie → synthesized `Authorization: Bearer` header
                                  (an explicit Authorization header always wins — never overridden)
   2. Spring Security             RS256 signature + expiry validated against auth's JWK Set,
                                  iss/aud claims enforced (JwtConfig)
   3. Route proxy                 request forwarded to the matching service,
                                  bearer header rides along
```

The JWK Set is fetched lazily on first use and cached — auth does not need to be up when the
gateway starts.

## Routing

Routes are matched **in declaration order** (`src/main/resources/application.properties`);
service-specific routes must stay above the auth catch-all (`/api/**`) or auth swallows them
and answers 404.

| # | Route | Path(s) | Upstream (env var) |
|---|---|---|---|
| 0 | `genai` | `/api/v1/chat/**`, `/api/v1/job-postings/**` | `GENAI_URI` |
| 1 | `document` | `/api/v1/profile/**`, `/api/v1/documents/**` | `DOCUMENT_URI` |
| 2 | `application` | `/api/v1/applications/**` | `APPLICATION_URI` |
| 3 | `email` | `/api/v1/email/**` | `EMAIL_URI` |
| 4 | `auth` (catch-all) | `/api/**` | `AUTH_URI` |
| 5 | `web-client` | `/**` (SPA shell + assets) | `WEB_CLIENT_URI` |

## Security policy

| Surface | Rule | Why |
|---|---|---|
| `/api/**` | Authenticated (valid access JWT) | The default — everything API-shaped needs a token |
| `POST /api/v1/auth/{login,register,refresh}` | Public | You cannot present a token to obtain your first token |
| `GET /api/v1/auth/.well-known/jwks.json` | Public | Public verification material by design |
| `GET /api/v1/email/connections/gmail/callback` | Public | Google redirects the browser here with no cookie; the signed OAuth `state` param (validated by the email service) is the trust anchor |
| `/actuator/health/**`, `/actuator/prometheus` | Public *(management port only)* | k8s probes and Prometheus scrapes must never need a token |
| Any other `/actuator/**` | **denyAll** | `/actuator/gateway/routes` enumerates internal topology — no end-user JWT should grant access to the gateway's own operational surface |
| Everything else (`/**`) | Public | SPA shell and static assets |

Also of note:

- **Stateless** — no sessions, CSRF disabled (auth is by bearer token, not an ambient session
  cookie the browser attaches automatically to cross-site requests).
- **`iss`/`aud` enforced** — the decoder in `JwtConfig` requires `iss=https://jobready-auth` and
  `aud=jobready`, matching what auth stamps into every access token.
- **Cookie translation skips the public auth paths** — login/register/refresh must reach auth
  without a synthesized header so an expired cookie can't shadow the refresh flow.

## Configuration

All settings have compose/cluster-friendly defaults; override via environment.

| Variable | Default | Purpose |
|---|---|---|
| `AUTH_JWKS_URL` | `http://auth:8080/api/v1/auth/.well-known/jwks.json` | Where to fetch auth's public keys |
| `AUTH_URI` | `http://auth:8080` | Auth upstream |
| `APPLICATION_URI` | `http://application:8080` | Application upstream |
| `DOCUMENT_URI` | `http://document:8080` | Document upstream |
| `EMAIL_URI` | `http://email:8080` | Email upstream |
| `GENAI_URI` | `http://genai:8000` | GenAI upstream |
| `WEB_CLIENT_URI` | `http://web-client:80` | Frontend upstream |
| `SERVER_PORT` | `8081` | Public app port |
| `APP_VERSION` | `dev` | Image tag injected by Helm — exported as the `app_info{version}` metric |

**Ports.** App traffic on **8081**; health + metrics on the separate **management port 8090**
(`/actuator/health`, `/actuator/prometheus`) — the ingress cannot reach 8090 by construction,
since Kubernetes Services only route the app port.

**Proxy transport.** Upstreams are proxied over plain HTTP/1.1
(`spring.http.clients.imperative.factory=simple`) — the default JDK client attempts an h2c
upgrade on every request, which uvicorn (genai) rejects, dropping the body of every proxied
POST to the Python service.

## Running

### Via Docker Compose (recommended — full stack)

From the repo root:

```bash
docker compose up --build gateway
```

### Standalone

```bash
cd services/gateway
./mvnw spring-boot:run     # needs auth reachable for JWKS once traffic arrives
```

> Requires **JDK 21** (`JAVA_HOME`) — palantir-java-format crashes on newer JDKs.

## Testing & Quality

```bash
./mvnw test                # unit tests (incl. CookieToBearerFilter behaviour)
./mvnw verify              # build + test + package — mirrors CI (Spotless + Checkstyle)
./mvnw spotless:apply      # auto-fix formatting
```

The end-to-end suite ([`e2e_tests/`](../../e2e_tests/)) exercises the gateway as the stack's
single entry point — including the cookie→Bearer translation and refresh-token flows that unit
tests structurally cannot cover.

## Source map

```
src/main/java/com/jobready/gateway/
├── GatewayApplication.java            # Entry point
├── security/
│   ├── SecurityConfig.java            # Filter chain: public paths, /api auth, actuator lockdown
│   ├── CookieToBearerFilter.java      # jr_access cookie → Authorization: Bearer (pre-security)
│   ├── JwtConfig.java                 # JWKS decoder + iss/aud validation
│   └── AppInfoConfig.java             # app_info{version} gauge for release correlation
└── config/
    └── HttpClientConfig.java          # Upstream HTTP client tuning
src/main/resources/application.properties   # Routes (order matters!) + management config
```
