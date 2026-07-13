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

## Identity strategy — why this is a homemade service (and not Auth0/Keycloak)

This service is a **self-hosted OIDC-shaped issuer**: it signs its own RS256 access tokens,
publishes a JWK Set at `/api/v1/auth/.well-known/jwks.json`, and every other service verifies
tokens locally against that key (defense in depth). We deliberately **did not** adopt an external
IdP (Auth0/Keycloak/GitHub). The reasoning, so it isn't re-litigated:

- **Rubric fit.** The course requires ≥3 Spring Boot microservices (`auth`, `application`,
  `document`). An external IdP would hollow this service out to almost nothing (Auth0 owns
  register/login/JWKS; only just-in-time profile provisioning — a *domain* concern — would remain),
  putting the 3-service requirement at risk.
- **Dependency footprint.** Prod is a namespace-scoped TUM Rancher tenant. A homemade issuer needs
  **zero egress**; Auth0 would add an external SaaS dependency, per-env callback URLs, a client
  secret, and a **BFF rewrite** (Auth0's redirect flow otherwise dumps tokens into the browser and
  breaks our HttpOnly-cookie model). *(Egress from the prod namespace was verified 2026-07-02, so
  outbound SMTP for password reset / email verification below is unblocked.)*
- **What we give up by not using Auth0:** MFA, password reset, email verification, breached-password
  detection, brute-force protection, audit logging, and pentested code — all things we now own. The
  hardening roadmap below closes the ones that matter for this project; the rest are documented as
  accepted limitations.

**Security is not the crypto — it's the hardening + who owns the bugs.** Our RS256/JWKS/BCrypt
primitives are industry-standard and as sound as Auth0's. The risk of "homemade" is the checks you
forget (see the `iss`/`aud` gap below), not the algorithms.

## Security posture (deliberate decisions — change consciously)

- **Token transport:** access + refresh JWT/opaque tokens are delivered **only** as HttpOnly
  cookies (`jr_access` `Path=/`, `jr_refresh` `Path=/api/v1/auth`), `Secure; SameSite=Strict`.
  Never in a response body, never readable by JS.
- **CSRF is disabled** (`SecurityConfig.csrf.disable()`) **on purpose** — our CSRF mitigation is
  `SameSite=Strict` cookies + a same-origin BFF/gateway. This is a *decision*, not a default. If we
  ever loosen to `SameSite=Lax`, we MUST reintroduce CSRF tokens for state-changing requests.
- **Token lifetimes:** access TTL `900s` (15 min — short by design; bounds the blast radius of a
  leaked token, which cannot be revoked before expiry). Refresh TTL `604800s` (7 days), stored in
  Redis and revocable on logout.
- **Verification:** RS256 + JWKS; the gateway rejects unauthenticated traffic at the edge and each
  service re-verifies the signature (defense in depth). Services are additionally isolated with
  NetworkPolicies so a stolen/forged header can't bypass the gateway pod-to-pod.

## Hardening roadmap (committed 2026-07-02)

Do these to keep "homemade" a defensible security posture rather than an accidental one.

### Tier 1 — cheap, high-value (do all)
- [ ] **Add `iss` + `aud` claims and enforce them in every verifier.** *Known gap:* access tokens
  currently carry only `sub`, `email`, `iat`, `exp` (`JwtServiceImpl`), and both decoders
  (`JwtConfig` in `auth` and `application`) use default validators that check **signature + expiry
  only** — no issuer/audience. Add `JwtValidators.createDefaultWithIssuer(...)` + an audience
  `OAuth2TokenValidator`.
- [ ] **Login rate-limiting / lockout** — Redis counter keyed by IP+email, lockout with TTL after N
  failures.
- [ ] **User-enumeration defense** — identical `401` for "no such user" vs "wrong password";
  register must not reveal whether an email already exists.
- [ ] **Password policy** — enforce min length (≥12) + reject a bundled top-N common-passwords list
  (no egress; poor-man's breached-password check).
- [x] **Refresh-token rotation** — done. `AuthServiceImpl.refresh` validates the presented token,
  deletes its Redis key, and issues a fresh access + refresh pair; replaying a rotated token gets
  a `401`. Remaining gap: replay is only *rejected*, not *detected* — see reuse detection below.

### Tier 2 — course-worthy, medium effort
- [ ] **Password reset** — single-use, time-limited token in Redis + emailed link. *Needs an
  outbound transactional mail sender (Spring Mail/SMTP or the Gmail API) — the existing `email`
  service is inbound-only. Egress is confirmed available.*
- [ ] **Email verification** — same machinery as reset (~80% shared code).
- [ ] **Refresh-token reuse detection** — the remaining gap now that rotation is in place. Today an
  already-rotated token is simply rejected (`401`); it should be treated as theft, invalidating the
  whole token family. Requires storing a family/session ID alongside each refresh token in Redis
  (currently keys are `refresh:<token>` → `userId` with no lineage), so a replay can revoke every
  descendant token in one sweep.
- [ ] **Security audit logging** — structured events (login success/fail, password change, reset);
  export failed-login counts as a Prometheus metric → Grafana alert (also satisfies the project
  observability requirement).

### Tier 3 — deferred (documented, not scheduled)
- TOTP MFA · automated signing-key rotation (multi-key JWKS overlap) · HSTS + security headers at
  the gateway. Revisit only if time allows or the service moves toward real production use.
