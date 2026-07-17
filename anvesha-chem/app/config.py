from __future__ import annotations

from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """All configuration loaded from environment / .env file."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    # Novelty scoring
    novelty_threshold: float = 0.7

    # External API toggles
    pubchem_enabled: bool = True
    chembl_enabled: bool = True
    surechembl_enabled: bool = False

    # Offline demo mode — skips all external API calls
    offline_mode: bool = False

    # RAG service integration URL
    rag_service_url: str = "http://localhost:8000"

    # HTTP client
    http_timeout: float = 10.0
    http_max_retries: int = 3

    # LRU cache
    novelty_cache_size: int = 256

    # Logging
    log_level: str = "INFO"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
