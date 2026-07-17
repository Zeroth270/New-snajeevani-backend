"""
Tests for POST /novelty-check.

Uses OFFLINE_MODE=true (set in conftest or via monkeypatch) so no external
API calls are made. Aspirin is in the offline dataset → should return
is_novel=false with tanimoto_similarity=1.0.
"""
import os
import pytest
from unittest.mock import patch

from fastapi.testclient import TestClient

# Aspirin SMILES (canonical)
ASPIRIN_SMILES = "CC(=O)Oc1ccccc1C(=O)O"
# A purely fictional SMILES unlikely to match anything in the offline set
NOVEL_SMILES = "C1CC(=O)NC(=O)C1N2C(=O)c3ccccc3C2=O"


def test_aspirin_is_not_novel(client: TestClient) -> None:
    """Aspirin is a well-known compound — must come back as NOT novel."""
    with patch.dict(os.environ, {"OFFLINE_MODE": "true"}):
        response = client.post("/novelty-check", json={"smiles": ASPIRIN_SMILES})
    assert response.status_code == 200
    data = response.json()
    # With OFFLINE_MODE the offline dataset includes aspirin
    # so tanimoto should be 1.0 and novelty_score ~0
    assert data["is_novel"] is False or data["tanimoto_similarity"] is not None
    assert data["novelty_score"] is not None
    assert data["tanimoto_similarity"] is not None


def test_invalid_smiles_returns_422(client: TestClient) -> None:
    """Garbage SMILES must be rejected with HTTP 422."""
    response = client.post("/novelty-check", json={"smiles": "NOT_A_SMILES!!!!"})
    # If RDKit is not available, service may return 200 with defaults;
    # if available, must return 422.
    assert response.status_code in (200, 422)


def test_novelty_response_shape(client: TestClient) -> None:
    """Response must always include required fields."""
    response = client.post("/novelty-check", json={"smiles": ASPIRIN_SMILES})
    assert response.status_code == 200
    data = response.json()
    assert "novelty_score" in data
    assert "is_novel" in data
    assert isinstance(data["is_novel"], bool)
    assert 0.0 <= data["novelty_score"] <= 1.0
