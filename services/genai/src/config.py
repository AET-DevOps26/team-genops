from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# Resolve the repo-root .env regardless of where the process is launched from
_ROOT_ENV = Path(__file__).parent.parent.parent.parent / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=str(_ROOT_ENV), extra="ignore")

    # Database
    database_url: str = "postgresql://postgres:postgres@localhost:5432/jobready"

    # OpenRouter / LangChain
    openrouter_api_key: str
    openrouter_model: str = "openai/gpt-oss-120b"
    openrouter_embedding_model: str = "qwen/qwen3-embedding-8b"
    openrouter_base_url: str = "https://openrouter.ai/api/v1"

    # Langfuse — LLM observability. Blank keys disable tracing (a no-op), so the
    # service still runs locally without a Langfuse instance. The SDK also reads
    # these from LANGFUSE_HOST/PUBLIC_KEY/SECRET_KEY env vars directly.
    langfuse_host: str = "http://langfuse-web:3000"
    langfuse_public_key: str = ""
    langfuse_secret_key: str = ""

    # JWT — public key fetched from auth service to verify Bearer tokens
    auth_jwks_url: str = "http://auth:8080/api/v1/auth/.well-known/jwks.json"

    # Document service (profile data)
    document_service_url: str = "http://document:8080"
    profile_enabled: bool = False

    # Application service (job/company context for tailored documents)
    application_service_url: str = "http://application:8080"


settings = Settings()  # type: ignore[call-arg]  # openrouter_api_key comes from the env
