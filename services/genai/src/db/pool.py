from pathlib import Path

import psycopg
from psycopg_pool import AsyncConnectionPool
from pgvector.psycopg import register_vector_async

from src.config import settings

# sql/ lives at the service root — two levels up from src/db/
_SCHEMA_SQL = (Path(__file__).parent.parent.parent / "sql" / "schema.sql").read_text()

pool: AsyncConnectionPool | None = None


async def init_db() -> None:
    global pool

    # Step 1 — run schema SQL with a plain connection (creates the vector extension)
    async with await psycopg.AsyncConnection.connect(settings.database_url) as conn:
        await conn.execute(_SCHEMA_SQL)

    # Step 2 — open the pool now that the vector extension exists
    pool = AsyncConnectionPool(
        settings.database_url,
        configure=_configure_conn,
        open=False,
    )
    await pool.open()


async def close_db() -> None:
    if pool:
        await pool.close()


async def get_conn():
    """FastAPI dependency — yields a connection from the pool."""
    async with pool.connection() as conn:
        yield conn


async def _configure_conn(conn) -> None:
    """Called by the pool for every new connection — registers pgvector codec."""
    await register_vector_async(conn)
