"""Application configuration, loaded from environment variables.

All settings are read once at import time via pydantic-settings. Secrets (DB URL,
Google client secret, the token-encryption key) come from the environment — never
hard-coded — per the project's secret-handling rules.
"""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


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
    email_token_enc_key: str = "dev-only-change-me"

    # Signing key for the OAuth `state` token (HMAC). Bound to the user + a nonce.
    state_signing_key: str = "dev-only-change-me"
    state_ttl_seconds: int = 600

    # Background poller.
    email_poll_interval_seconds: int = 300
    gmail_max_results: int = 25


@lru_cache
def get_settings() -> Settings:
    return Settings()
