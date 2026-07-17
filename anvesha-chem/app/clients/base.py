"""
Base interface for chemistry database clients.
All concrete clients implement this ABC so any one can be mocked or
swapped without touching novelty_service.py.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class SearchResult:
    """Minimal result from a chemistry database lookup."""
    compound_id: str          # e.g. PubChem CID, ChEMBL ID
    source: str               # "PUBCHEM", "CHEMBL", "SURECHEMBL", "OFFLINE"
    tanimoto_similarity: float  # 0.0 – 1.0


class ChemDatabaseClient(ABC):
    """
    Pluggable chemistry database client.

    Implementors: PubChemClient, ChemblClient, SureChemblClient, OfflineClient.
    Any implementation can be disabled via settings without changing the
    novelty_service.py orchestration logic.
    """

    @property
    @abstractmethod
    def source_name(self) -> str:
        """Human-readable source label stored in the DB."""
        ...

    @abstractmethod
    async def search(self, smiles: str) -> SearchResult | None:
        """
        Search the database for the closest known compound to *smiles*.
        Returns None if the database is unreachable or no hit is found.
        """
        ...
