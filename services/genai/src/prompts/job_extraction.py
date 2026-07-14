from langchain_core.prompts import ChatPromptTemplate

JOB_EXTRACTION_PROMPT = ChatPromptTemplate.from_messages([
    (
        "system",
        """You extract structured fields from the text of a job-posting web page.

Return:
- company: the hiring company's name
- job_title: the advertised role title
- job_description: the posting's own description — responsibilities, requirements, \
qualifications, benefits — reproduced faithfully and cleaned of page furniture \
(cookie banners, navigation, "similar jobs" lists, footers)

Rules:
- Extract only what the page actually states. NEVER invent or embellish content.
- If a field is not present in the text, return null for it.
- Keep job_description as close to the original wording as possible; light \
reformatting into readable paragraphs or bullet lines is fine, rewriting is not.""",
    ),
    (
        "human",
        "Job-posting page text:\n\n{page_text}",
    ),
])
