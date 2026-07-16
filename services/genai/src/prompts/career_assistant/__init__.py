from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

from src.prompts.career_assistant.system.base import SYSTEM_PROMPT

career_assistant_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
    ]
)
