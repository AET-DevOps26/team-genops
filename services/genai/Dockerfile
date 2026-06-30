FROM python:3.12-slim

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Copy dependency files first — cached layer rebuilt only when deps change
COPY pyproject.toml uv.lock ./

# Install production dependencies without source code
RUN uv sync --frozen --no-dev --no-install-project

# Copy application source and SQL schema
COPY src/ ./src/
COPY sql/ ./sql/

# Install the project itself
RUN uv sync --frozen --no-dev

EXPOSE 8000

CMD ["uv", "run", "uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000"]
