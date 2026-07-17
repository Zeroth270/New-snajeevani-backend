"""
ChEMBL REST API similarity client.

Uses the ChEMBL /similarity/{smiles}/{threshold} endpoint to find the
closest known bioactive compound.

Docs: https://www.ebi.ac.uk/chembl/api/data/similarity/
"""
from __future__ import annotations

import logging
from urllib.parse import quote

import httpx
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

from app.clients.base import ChemDatabaseClient, SearchResult
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

_BASE = "https://www.ebi.ac.uk/chembl/api/data"


class ChemblClient(ChemDatabaseClient):

    @property
    def source_name(self) -> str:
        return "CHEMBL"

    @retry(
        stop=stop_after_attempt(settings.http_max_retries),
        wait=wait_exponential(multiplier=1, min=1, max=8),
        retry=retry_if_exception_type((httpx.TimeoutException, httpx.NetworkError)),
        reraise=False,
    )
    async def search(self, smiles: str) -> SearchResult | None:
        encoded = quote(smiles, safe="")
        threshold = 70  # Tanimoto similarity threshold (0-100)

        url = f"{_BASE}/similarity/{encoded}/{threshold}.json?limit=1"
        try:
            async with httpx.AsyncClient(timeout=settings.http_timeout) as client:
                resp = await client.get(url)
                if resp.status_code != 200:
                    return None

                data = resp.json()
                molecules = data.get("molecules", [])
                if not molecules:
                    return None

                mol = molecules[0]
                chembl_id = mol.get("molecule_chembl_id", "UNKNOWN")
                similarity = mol.get("similarity", 70) / 100.0  # normalise to 0-1

                logger.debug("ChEMBL hit: %s (similarity %.3f) for %s",
                             chembl_id, similarity, smiles[:40])
                return SearchResult(compound_id=chembl_id, source=self.source_name,
                                    tanimoto_similarity=similarity)

        except Exception as exc:
            logger.warning("ChEMBL search error: %s", exc)
            return None
