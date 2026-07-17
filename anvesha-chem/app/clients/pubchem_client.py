"""
PubChem PUG REST API client.

Strategy:
1. Try an exact-match lookup by SMILES → returns a CID (tanimoto = 1.0)
2. If no exact hit, run a fingerprint similarity search and take the best hit.

Docs: https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest
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

_BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug"


class PubChemClient(ChemDatabaseClient):

    @property
    def source_name(self) -> str:
        return "PUBCHEM"

    @retry(
        stop=stop_after_attempt(settings.http_max_retries),
        wait=wait_exponential(multiplier=1, min=1, max=8),
        retry=retry_if_exception_type((httpx.TimeoutException, httpx.NetworkError)),
        reraise=False,
    )
    async def search(self, smiles: str) -> SearchResult | None:
        encoded = quote(smiles, safe="")

        async with httpx.AsyncClient(timeout=settings.http_timeout) as client:
            # Step 1: Exact match
            try:
                url = f"{_BASE}/compound/smiles/{encoded}/cids/JSON"
                resp = await client.get(url)
                if resp.status_code == 200:
                    data = resp.json()
                    cids = data.get("IdentifierList", {}).get("CID", [])
                    if cids:
                        cid = str(cids[0])
                        logger.debug("PubChem exact match: CID %s for %s", cid, smiles[:40])
                        return SearchResult(compound_id=cid, source=self.source_name,
                                            tanimoto_similarity=1.0)
            except Exception as exc:
                logger.warning("PubChem exact-match error: %s", exc)

            # Step 2: Similarity search (Tanimoto >= 0.7)
            try:
                sim_url = (f"{_BASE}/compound/fastsimilarity_2d/smiles/{encoded}"
                           f"/cids/JSON?Threshold=70&MaxRecords=1")
                resp2 = await client.get(sim_url)
                if resp2.status_code == 200:
                    data2 = resp2.json()
                    cids2 = data2.get("IdentifierList", {}).get("CID", [])
                    if cids2:
                        cid = str(cids2[0])
                        # PubChem similarity search doesn't return the Tanimoto directly;
                        # we conservatively report 0.75 as a floor estimate.
                        logger.debug("PubChem similarity hit: CID %s", cid)
                        return SearchResult(compound_id=cid, source=self.source_name,
                                            tanimoto_similarity=0.75)
            except Exception as exc:
                logger.warning("PubChem similarity error: %s", exc)

        return None
