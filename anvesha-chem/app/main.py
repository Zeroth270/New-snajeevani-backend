from __future__ import annotations

import logging

from fastapi import FastAPI

from app.config import get_settings
from app.routers import extract, novelty, health

settings = get_settings()

logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)

app = FastAPI(
    title="Anvesha Chemistry Service",
    description=(
        "Internal stateless microservice for molecule extraction and structural novelty checking. "
        "Called exclusively by the Spring Boot anvesha-core backend — never directly by the frontend."
    ),
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

app.include_router(health.router)
app.include_router(extract.router)
app.include_router(novelty.router)
