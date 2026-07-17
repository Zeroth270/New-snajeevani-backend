from __future__ import annotations

from pydantic import BaseModel, Field


class ExtractRequest(BaseModel):
    paper_text: str = Field(..., description="Full text of the research paper to scan for chemical names")


class MoleculeResult(BaseModel):
    extracted_name_raw: str = Field(..., description="Chemical name as it appeared in the text")
    iupac_name: str | None = Field(None, description="Normalised IUPAC name from OPSIN (if parseable)")
    smiles: str | None = Field(None, description="SMILES string from OPSIN (if parseable)")
    extraction_confidence: float = Field(..., ge=0.0, le=1.0,
                                         description="Heuristic confidence score 0–1")


class ExtractResponse(BaseModel):
    molecules: list[MoleculeResult]
