"""
Novelty scoring service.

Pipeline:
    1. Parse SMILES with RDKit (422 if invalid).
    2. Generate Morgan fingerprint (radius=2, 2048 bits).
    3. Query enabled ChemDatabaseClients in parallel.
    4. Compute novelty_score = 1 - max(tanimoto across all sources).
    5. Cache results by canonical SMILES to avoid re-hitting APIs on demo reruns.
"""
from __future__ import annotations

import asyncio
import logging
from functools import lru_cache

from fastapi import HTTPException

from app.clients.base import ChemDatabaseClient, SearchResult
from app.clients.chembl_client import ChemblClient
from app.clients.offline_client import OfflineClient
from app.clients.pubchem_client import PubChemClient
from app.clients.surechembl_client import SureChemblClient
from app.config import get_settings
from app.models.novelty import NoveltyResponse

logger = logging.getLogger(__name__)
settings = get_settings()

# RDKit is an optional hard dependency — imported lazily to allow the service
# to start even if rdkit-pypi wheels are still being fetched (CI bootstrap).
try:
    from rdkit import Chem
    from rdkit.Chem import AllChem, DataStructs
    RDKIT_AVAILABLE = True
except ImportError:
    RDKIT_AVAILABLE = False
    logger.warning("RDKit not available — novelty checks will return defaults")


# ── Client registry ──────────────────────────────────────────────────────────

def _build_clients() -> list[ChemDatabaseClient]:
    """Return the list of enabled database clients based on settings."""
    if settings.offline_mode:
        logger.info("OFFLINE_MODE enabled — using only offline dataset")
        return [OfflineClient()]

    clients: list[ChemDatabaseClient] = []
    if settings.pubchem_enabled:
        clients.append(PubChemClient())
    if settings.chembl_enabled:
        clients.append(ChemblClient())
    if settings.surechembl_enabled:
        clients.append(SureChemblClient())

    # Always include offline as the last fallback
    clients.append(OfflineClient())
    return clients


_CLIENTS: list[ChemDatabaseClient] = _build_clients()


# ── LRU cache on canonical SMILES ────────────────────────────────────────────

@lru_cache(maxsize=settings.novelty_cache_size)
def _cached_novelty(canonical_smiles: str) -> NoveltyResponse:
    """Synchronous wrapper — called from async context via run_in_executor."""
    return asyncio.get_event_loop().run_until_complete(
        _compute_novelty(canonical_smiles))


# ── Main entry point ─────────────────────────────────────────────────────────

async def check_novelty(smiles: str) -> NoveltyResponse:
    """
    Main novelty check entry point.
    Validates SMILES with RDKit, canonicalises it, and queries all
    enabled database clients. Results are cached per canonical SMILES.
    """
    if not RDKIT_AVAILABLE:
        # Graceful degradation: treat as novel if RDKit not installed
        logger.warning("RDKit unavailable — returning mock novel response")
        return NoveltyResponse(
            novelty_score=1.0, is_novel=True,
            closest_match_source=None, closest_match_id=None,
            tanimoto_similarity=None,
        )

    # Step 1: Parse and canonicalise
    mol = Chem.MolFromSmiles(smiles)
    if mol is None:
        raise HTTPException(status_code=422,
                            detail=f"Invalid SMILES string: '{smiles}'")
    canonical = Chem.MolToSmiles(mol, canonical=True)

    # Step 2: Return cached result if available
    cached = _novelty_cache.get(canonical)
    if cached is not None:
        logger.debug("Cache hit for canonical SMILES: %s", canonical[:50])
        return cached

    result = await _compute_novelty(canonical)

    # Cache the result
    _novelty_cache[canonical] = result
    # Trim cache if over size (simple FIFO eviction for demo)
    if len(_novelty_cache) > settings.novelty_cache_size:
        oldest_key = next(iter(_novelty_cache))
        del _novelty_cache[oldest_key]

    return result


# Simple dict-based TTL-less cache (sufficient for a hackathon)
_novelty_cache: dict[str, NoveltyResponse] = {}


async def _compute_novelty(canonical_smiles: str) -> NoveltyResponse:
    """Query all clients and compute the final novelty score."""
    # Compute Morgan fingerprint
    mol = Chem.MolFromSmiles(canonical_smiles)
    fp = AllChem.GetMorganFingerprintAsBitVect(mol, radius=2, nBits=2048)

    # Run all client searches in parallel
    tasks = [client.search(canonical_smiles) for client in _CLIENTS]
    results: list[SearchResult | None] = await asyncio.gather(*tasks, return_exceptions=False)

    best: SearchResult | None = None
    best_tanimoto = 0.0

    for result in results:
        if result is None:
            continue
        if result.tanimoto_similarity > best_tanimoto:
            best_tanimoto = result.tanimoto_similarity
            best = result

    novelty_score = round(1.0 - best_tanimoto, 3)
    is_novel = novelty_score >= settings.novelty_threshold

    return NoveltyResponse(
        novelty_score=novelty_score,
        is_novel=is_novel,
        closest_match_source=best.source if best else None,
        closest_match_id=best.compound_id if best else None,
        tanimoto_similarity=round(best_tanimoto, 3) if best else None,
    )
