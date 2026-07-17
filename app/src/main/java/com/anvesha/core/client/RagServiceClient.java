package com.anvesha.core.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;

/**
 * HTTP client that bridges Spring Boot -> Python FastAPI RAG microservice.
 * Indexes scientific paper texts in the local Qdrant Vector database.
 */
@Slf4j
@Component
public class RagServiceClient {

    private final WebClient webClient;

    public RagServiceClient(@Qualifier("ragServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Send paper text to RAG service for parsing, chunking, and embedding indexing.
     * Operates fail-open (catches exceptions and logs warning) to protect the main pipeline
     * in case the RAG vector service is temporarily unreachable.
     */
    public void indexPaper(String text, String title) {
        try {
            log.info("Submitting text for RAG indexing under title: '{}'", title);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            // Represent raw string content as a virtual text file resource
            builder.part("file", new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return "paper.txt";
                }
            }, MediaType.TEXT_PLAIN);

            builder.part("title", title);

            MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();

            webClient.post()
                    .uri("/documents/index")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(multipartBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("RAG Service successfully completed indexing for paper '{}'", title);
        } catch (Exception ex) {
            log.warn("RAG indexing failed for paper '{}': {} (main workflow continuing)", title, ex.getMessage());
        }
    }
}
