from pydantic_settings import BaseSettings
from pydantic import Field
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings loaded from environment variables and .env file."""

    GEMINI_API_KEY: str = Field(
        default="",
        description="Google Gemini API key for AI-powered legal advice",
    )
    APP_ENV: str = Field(
        default="development",
        description="Application environment: development | staging | production",
    )
    DEBUG: bool = Field(
        default=True,
        description="Enable debug mode",
    )

    # CORS settings
    CORS_ORIGINS: list[str] = Field(
        default=["http://localhost:3000", "http://localhost:5173", "http://localhost:8080"],
        description="Allowed CORS origins",
    )

    # API settings
    API_V1_PREFIX: str = "/api/v1"

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "case_sensitive": True,
    }


@lru_cache()
def get_settings() -> Settings:
    """Return cached application settings singleton."""
    return Settings()
