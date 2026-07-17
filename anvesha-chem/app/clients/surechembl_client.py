"""
SureChEMBL client stub.

SureChEMBL (https://www.surechembl.org/) extracts chemical structures
from patent documents. Their public search API has been subject to rate
limits and intermittent unavailability.

SWAP NOTE:
    For a production deployment, this client should be replaced with a
    local reader of a bulk-downloaded SureChEMBL dataset (available as
    annual FTP dumps). The interface (search → SearchResult | None) is
    identical; only the implementation changes.
"""
from __future__ import annotations

import logging

from app.clients.base import ChemDatabaseClient, SearchResult

logger = logging.getLogger(__name__)


class SureChemblClient(ChemDatabaseClient):

    @property
    def source_name(self) -> str:
        return "SURECHEMBL"

    async def search(self, smiles: str) -> SearchResult | None:
        # SureChEMBL live API is unreliable for hackathon demos.
        # This stub logs a warning and returns None so the pipeline
        # gracefully degrades to PubChem + ChEMBL results.
        #
        # TO SWAP: implement a local SQLite/parquet reader of the
        # SureChEMBL bulk download here, keeping the same interface.
        logger.debug("SureChEMBL client is a stub — no search performed for %s", smiles[:40])
        return None
