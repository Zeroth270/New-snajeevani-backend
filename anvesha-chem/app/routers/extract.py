from fastapi import APIRouter
from app.models.extract import ExtractRequest, ExtractResponse, MoleculeResult
from app.services.extraction_service import extract_molecules_from_text

router = APIRouter(tags=["Chemistry"])


@router.post("/extract-molecules", response_model=ExtractResponse)
async def extract_molecules(req: ExtractRequest) -> ExtractResponse:
    """
    Extract chemical structures from research paper text.

    Runs heuristic IUPAC NER over the text, converts candidate names to
    SMILES via OPSIN, and returns only candidates OPSIN could parse.
    """
    results = extract_molecules_from_text(req.paper_text)
    molecules = [
        MoleculeResult(
            extracted_name_raw=r.extracted_name_raw,
            iupac_name=r.iupac_name,
            smiles=r.smiles,
            extraction_confidence=r.extraction_confidence,
        )
        for r in results
    ]
    return ExtractResponse(molecules=molecules)
