from fastapi import APIRouter

router = APIRouter(tags=["Health"])


@router.get("/health")
async def health() -> dict:
    """Liveness check — returns 200 with service status."""
    return {"status": "ok", "service": "anvesha-chem"}
