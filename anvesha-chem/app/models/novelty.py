from __future__ import annotations

from pydantic import BaseModel, Field


class NoveltyRequest(BaseModel):
    smiles: str = Field(..., description="SMILES string to check for structural novelty")


class NoveltyResponse(BaseModel):
    novelty_score: float = Field(..., ge=0.0, le=1.0,
                                  description="1 - max(Tanimoto similarity across all sources). 1=fully novel, 0=known")
    is_novel: bool = Field(..., description="True if novelty_score >= configured threshold (default 0.7)")
    closest_match_source: str | None = Field(None, description="Database of the closest hit: PUBCHEM, CHEMBL, SURECHEMBL, OFFLINE")
    closest_match_id: str | None = Field(None, description="Identifier of the closest known compound")
    tanimoto_similarity: float | None = Field(None, ge=0.0, le=1.0,
                                               description="Tanimoto similarity to the closest match")
