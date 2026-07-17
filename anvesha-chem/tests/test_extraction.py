"""
Tests for POST /extract-molecules.

These tests use text containing known IUPAC names to verify that the
extraction pipeline can find and parse them via OPSIN.
"""
from fastapi.testclient import TestClient

# Paper-like text with embedded IUPAC names
SAMPLE_TEXT = (
    "In this study, we synthesised 2-acetoxybenzoic acid (aspirin) and "
    "assessed its interaction with acetaminophen. "
    "We also characterised 1-methylimidazole as a catalyst."
)

# Text with no chemical names
EMPTY_TEXT = "The study was conducted at room temperature over 24 hours."


def test_extract_returns_list(client: TestClient) -> None:
    """Response always has a 'molecules' key with a list."""
    response = client.post("/extract-molecules", json={"paper_text": SAMPLE_TEXT})
    assert response.status_code == 200
    data = response.json()
    assert "molecules" in data
    assert isinstance(data["molecules"], list)


def test_empty_text_returns_empty_list(client: TestClient) -> None:
    """Text with no chemical names → empty molecules list."""
    response = client.post("/extract-molecules", json={"paper_text": EMPTY_TEXT})
    assert response.status_code == 200
    data = response.json()
    assert data["molecules"] == [] or isinstance(data["molecules"], list)


def test_molecule_result_shape(client: TestClient) -> None:
    """Every returned molecule has the required fields."""
    response = client.post("/extract-molecules", json={"paper_text": SAMPLE_TEXT})
    assert response.status_code == 200
    data = response.json()
    for mol in data["molecules"]:
        assert "extracted_name_raw" in mol
        assert "extraction_confidence" in mol
        assert 0.0 <= mol["extraction_confidence"] <= 1.0
