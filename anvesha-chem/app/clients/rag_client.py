import logging
import httpx
from app.clients.base import ChemDatabaseClient, SearchResult
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

class RagClient(ChemDatabaseClient):
    """
    Retrieval-Augmented Generation (RAG) local document registry client.
    Queries the Sanjeevani RAG service to check if the molecule (by SMILES)
    was already disclosed in previously indexed institutional research papers.
    """

    @property
    def source_name(self) -> str:
        return "RAG"

    async def search(self, smiles: str) -> SearchResult | None:
        url = f"{settings.rag_service_url}/retrieve"
        try:
            logger.info("Querying local RAG vector DB index for SMILES '%s'...", smiles[:30])
            async with httpx.AsyncClient(timeout=settings.http_timeout) as client:
                # Retrieve context chunks matching this SMILES string
                response = await client.post(
                    url,
                    json={
                        "query": f"The chemical compound with SMILES structure code: {smiles}",
                        "top_k": 3,
                        "similarity_threshold": 0.5,
                        "use_mmr": False
                    }
                )
                if response.status_code != 200:
                    logger.warning("RAG service query failed with status %d", response.status_code)
                    return None
                
                data = response.json()
                # Safely parse retrieved chunks
                chunks = data.get("content", []) or data.get("chunks", []) or []
                if not chunks:
                    logger.info("No prior disclosures of '%s' found in local RAG vector DB index", smiles[:30])
                    return None
                
                # Check for high-similarity matches (e.g. >= 0.70)
                best_chunk = chunks[0]
                score = float(best_chunk.get("similarity_score", 0.0))
                
                # Filter out low-relevance semantic noise
                if score < 0.65:
                    logger.debug("RAG candidate match score %.3f below threshold", score)
                    return None

                doc_title = best_chunk.get("title") or "Prior Research Publication"
                doc_id = best_chunk.get("document_id") or "unknown"
                
                logger.warning("Local RAG disclosure alert! Found prior match in '%s' (vector score: %.3f)", doc_title, score)
                
                return SearchResult(
                    compound_id=f"Prior Disclosure: {doc_title} (Doc ID: {doc_id[:8]})",
                    source="RAG",
                    tanimoto_similarity=score
                )
        except Exception as e:
            logger.warning("Failed to query local RAG database client: %s", e)
            return None
