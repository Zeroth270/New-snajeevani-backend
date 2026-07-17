"""
Offline fallback client — a small bundled dataset of well-known molecules.

PURPOSE:
    When OFFLINE_MODE=true (or when venue Wi-Fi is unreliable during a
    hackathon demo), this client answers novelty queries from a local
    in-memory dataset. It computes exact SMILES match only (no external API).

    The dataset covers the most common reference molecules used to
    sanity-check the pipeline:
    - Aspirin, Caffeine, Ibuprofen, Paracetamol (known — not novel)
    - A handful of fictional novel structures for demo purposes

HOW TO EXTEND:
    Add entries to OFFLINE_MOLECULES as { "smiles": ..., "id": ..., "name": ... }.
    For a larger offline set, load from a bundled CSV/SQLite at startup.
"""
from __future__ import annotations

import logging

from app.clients.base import ChemDatabaseClient, SearchResult

logger = logging.getLogger(__name__)

# Canonical SMILES → (mock ID, source label)
OFFLINE_MOLECULES: dict[str, tuple[str, str]] = {
    # Aspirin
    "CC(=O)Oc1ccccc1C(=O)O":  ("OFFLINE-00001", "Aspirin"),
    # Caffeine
    "Cn1c(=O)c2c(ncn2C)n(c1=O)C": ("OFFLINE-00002", "Caffeine"),
    # Ibuprofen
    "CC(C)Cc1ccc(cc1)C(C)C(=O)O": ("OFFLINE-00003", "Ibuprofen"),
    # Paracetamol / Acetaminophen
    "CC(=O)Nc1ccc(O)cc1": ("OFFLINE-00004", "Paracetamol"),
    # Penicillin G
    "CC1(C)SC2C(NC1=O)C(=O)N2Cc1ccccc1": ("OFFLINE-00005", "Penicillin G"),
}


class OfflineClient(ChemDatabaseClient):

    @property
    def source_name(self) -> str:
        return "OFFLINE"

    async def search(self, smiles: str) -> SearchResult | None:
        canonical = smiles.strip()
        if canonical in OFFLINE_MOLECULES:
            mock_id, name = OFFLINE_MOLECULES[canonical]
            logger.debug("Offline hit: %s → %s", canonical[:40], name)
            return SearchResult(compound_id=mock_id, source=self.source_name,
                                tanimoto_similarity=1.0)
        logger.debug("No offline hit for: %s", canonical[:40])
        return None
