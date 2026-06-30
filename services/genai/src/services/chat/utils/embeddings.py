from src.llm.client import embeddings


async def embed_text(text: str) -> list[float]:
    """Embed a text string and return the vector."""
    return await embeddings.aembed_query(text)
