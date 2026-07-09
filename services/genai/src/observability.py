"""
Langfuse tracing wiring for the GenAI service.

Tracing rides on LangChain's callback system: one shared CallbackHandler is
attached to each LLM/agent invocation via a RunnableConfig. When no Langfuse
keys are configured the whole module degrades to a no-op, so the service runs
locally without a Langfuse instance (mirrors the ephemeral-JWT fallback in auth).
"""

import logging

from src.config import settings

logger = logging.getLogger(__name__)

# Tracing is only enabled when both keys are present. The Langfuse SDK also reads
# host/keys from LANGFUSE_HOST/PUBLIC_KEY/SECRET_KEY env vars, which docker-compose
# and Helm set from settings — we initialise explicitly to be independent of that.
tracing_enabled = bool(settings.langfuse_public_key and settings.langfuse_secret_key)

# A single handler is reused across requests. In Langfuse v3 the handler is
# stateless (trace context flows via OpenTelemetry), so sharing one instance is
# the recommended pattern.
_handler = None


def init_tracing() -> None:
    """Initialise the Langfuse client once at startup. No-op if unconfigured."""
    global _handler
    if not tracing_enabled:
        logger.info("Langfuse tracing disabled — no LANGFUSE_PUBLIC_KEY/SECRET_KEY set")
        return

    from langfuse import Langfuse
    from langfuse.langchain import CallbackHandler

    Langfuse(
        public_key=settings.langfuse_public_key,
        secret_key=settings.langfuse_secret_key,
        host=settings.langfuse_host,
    )
    _handler = CallbackHandler()
    logger.info("Langfuse tracing enabled (host=%s)", settings.langfuse_host)


def trace_config(
    *,
    user_id: str | None = None,
    session_id: str | None = None,
    tags: list[str] | None = None,
) -> dict:
    """
    Build a LangChain RunnableConfig carrying the Langfuse handler and trace
    attributes. Returns an empty dict when tracing is disabled, so it can be
    passed straight to `.ainvoke(..., config=trace_config(...))`.
    """
    if _handler is None:
        return {}

    metadata: dict[str, object] = {}
    if user_id:
        metadata["langfuse_user_id"] = user_id
    if session_id:
        metadata["langfuse_session_id"] = session_id
    if tags:
        metadata["langfuse_tags"] = tags

    return {"callbacks": [_handler], "metadata": metadata}


def flush() -> None:
    """Flush buffered events to Langfuse. Call on shutdown so nothing is lost."""
    if not tracing_enabled:
        return
    from langfuse import get_client

    get_client().flush()
