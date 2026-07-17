from fastapi import APIRouter
from app.models.novelty import NoveltyRequest, NoveltyResponse
from app.services.novelty_service import check_novelty

router = APIRouter(tags=["Chemistry"])


@router.post("/novelty-check", response_model=NoveltyResponse)
async def novelty_check(req: NoveltyRequest) -> NoveltyResponse:
    """
    Check the structural novelty of a molecule given its SMILES string.

    - Validates SMILES with RDKit (422 if invalid)
    - Queries PubChem, ChEMBL, and SureChEMBL (or offline dataset in demo mode)
    - Returns novelty_score = 1 - max(Tanimoto similarity across all sources)
    - is_novel = novelty_score >= configured threshold (default 0.7)
    """
    return await check_novelty(req.smiles)
