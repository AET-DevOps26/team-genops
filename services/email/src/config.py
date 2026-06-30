"""Application configuration, loaded from environment variables.

All settings are read once at import time via pydantic-settings. Secrets (DB URL,
Google client secret, the token-encryption and state-signing keys) come from the
environment in any real deployment. The placeholder defaults below exist only so the
service boots for local dev; using them is flagged loudly at startup (see
`warn_on_dev_secrets`) and must never reach production per the project's
secret-handling rules.
"""
import logging
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger(__name__)

_DEV_SECRET = "dev-only-change-me"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Database — points at the per-service email_db.
    db_url: str = "postgresql+psycopg2://user:user123@localhost:5432/email_db"

    # Auth service JWKS endpoint used to verify access tokens locally.
    auth_jwks_url: str = "http://auth:8080/api/v1/auth/.well-known/jwks.json"

    # Google OAuth2 credentials (from the Google Cloud console).
    google_client_id: str = ""
    google_client_secret: str = ""
    google_redirect_uri: str = "http://localhost:8001/api/v1/email/connections/gmail/callback"

    # Where the OAuth callback sends the browser once a connection is stored.
    frontend_redirect_url: str = "http://localhost:5173"

    # Symmetric key used to encrypt OAuth tokens at rest via pgcrypto.
    email_token_enc_key: str = _DEV_SECRET

    # Signing key for the OAuth `state` token (HMAC). Bound to the user + a nonce.
    state_signing_key: str = _DEV_SECRET
    state_ttl_seconds: int = 600

    # Background poller.
    email_poll_interval_seconds: int = 300
    gmail_max_results: int = 25


def warn_on_dev_secrets(settings: Settings) -> None:
    """Loudly flag any security-sensitive key still set to the dev placeholder."""
    insecure = [
        name
        for name in ("email_token_enc_key", "state_signing_key")
        if getattr(settings, name) == _DEV_SECRET
    ]
    if insecure:
        logger.warning(
            "INSECURE: %s using the built-in dev default — set explicit env vars "
            "before any non-local deployment.",
            ", ".join(insecure),
        )


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    warn_on_dev_secrets(settings)
    return settings
