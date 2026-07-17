package com.anvesha.core.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * HTTP client that bridges Spring Boot → Python FastAPI chemistry service.
 * All chemistry logic lives in the Python service; this class only does
 * serialisation, HTTP, and error handling.
 */
@Slf4j
@Component
public class PythonChemServiceClient {

    private final WebClient webClient;

    public PythonChemServiceClient(@Qualifier("chemServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    // ─────────────────── DTOs ───────────────────────────────────────────────

    public record ExtractMoleculesRequest(String paper_text) {}

    public record MoleculeResult(
            String extracted_name_raw,
            String iupac_name,
            String smiles,
            Double extraction_confidence
    ) {}

    public record ExtractMoleculesResponse(List<MoleculeResult> molecules) {}

    public record NoveltyCheckRequest(String smiles) {}

    public record NoveltyCheckResponse(
            Double novelty_score,
            Boolean is_novel,
            String closest_match_source,
            String closest_match_id,
            Double tanimoto_similarity
    ) {}

    // ─────────────────── API Calls ──────────────────────────────────────────

    /**
     * Call POST /extract-molecules on the Python service.
     * Returns an empty list if the service is unreachable (fail-open so the
     * paper still gets persisted; status will reflect FAILED).
     */
    public ExtractMoleculesResponse extractMolecules(String paperText) {
        try {
            return webClient.post()
                    .uri("/extract-molecules")
                    .bodyValue(new ExtractMoleculesRequest(paperText))
                    .retrieve()
                    .bodyToMono(ExtractMoleculesResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Python service /extract-molecules returned {}: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Chemistry service error during extraction: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Failed to reach Python chemistry service: {}", ex.getMessage());
            throw new RuntimeException("Chemistry service unreachable: " + ex.getMessage(), ex);
        }
    }

    /**
     * Call POST /novelty-check on the Python service for a single SMILES string.
     */
    public NoveltyCheckResponse checkNovelty(String smiles) {
        try {
            return webClient.post()
                    .uri("/novelty-check")
                    .bodyValue(new NoveltyCheckRequest(smiles))
                    .retrieve()
                    .bodyToMono(NoveltyCheckResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Python service /novelty-check returned {}: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Chemistry service error during novelty check: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Failed to reach Python chemistry service: {}", ex.getMessage());
            throw new RuntimeException("Chemistry service unreachable: " + ex.getMessage(), ex);
        }
    }
}
