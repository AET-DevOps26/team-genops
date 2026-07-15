from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from src.config import settings

# Chat model — used for all conversational turns
llm = ChatOpenAI(model=settings.openrouter_model, api_key=settings.openrouter_api_key, base_url=settings.openrouter_base_url, stream_usage=True, temperature=0.2)

# Embedding model — used to embed session summaries for pgvector similarity search
embeddings = OpenAIEmbeddings(
    model=settings.openrouter_embedding_model,
    api_key=settings.openrouter_api_key,
    base_url=settings.openrouter_base_url,
)
