from langchain_core.tools import tool

from src.services.profile_client import get_user_profile as _fetch_profile


def make_profile_tool(user_id: str):
    @tool
    async def get_user_profile() -> str:
        """
        Fetch the user's full career profile including work experience, education,
        skills, certifications, and languages.
        Use this when you need specific details about the user's background to
        write a cover letter, tailor a resume, or assess fit for a role.
        """
        return await _fetch_profile(user_id)

    return get_user_profile
